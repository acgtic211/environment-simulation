package digital.twin;

import org.neo4j.driver.*;
import org.tzi.use.api.UseApiException;
import org.tzi.use.uml.sys.MObjectState;
import utils.DTLogger;

import java.util.*;

import static org.neo4j.driver.Values.parameters;

/**
 * @author Paula Muñoz, Daniel Pérez - University of Málaga
 * Class that retrieves all instances of a USE model class and serializes them for storage in the data lake.
 */
public abstract class OutputManager {

    protected static final String IS_PROCESSED = "isProcessed";
    protected static final String WHEN_PROCESSED = "whenProcessed";

    protected final AttributeSpecification attributeSpecification;
    protected final TreeMap<String, AttributeSpecification> retrievedClassAttributes;
    protected final DTUseFacade useApi;
    private final String channel;
    private final String retrievedClass;
    private final String nodeLabel;

    /**
     * Default constructor. Constructors from subclasses must set the type of the attributes to serialize
     * using the attributeSpecification instance.
     * @param useApi USE API facade instance to interact with the currently displayed object diagram.
     * @param channel The channel this OutputManager is created from.
     * @param retrievedClass The class whose instances to retrieve and serialize.
     * @param nodeLabel Label to use for the nodes to be created.
     */
    public OutputManager(DTUseFacade useApi, String channel, String retrievedClass, String nodeLabel) {
        attributeSpecification = new AttributeSpecification();
        this.useApi = useApi;
        this.channel = channel;
        this.retrievedClass = retrievedClass;
        this.nodeLabel = nodeLabel;
        this.retrievedClassAttributes = new TreeMap();
        this.retrievedClassAttributes.put(retrievedClass, attributeSpecification);
    }

    public OutputManager(DTUseFacade useApi, String channel, String retrievedClass, String nodeLabel, AttributeSpecification classAttributes) {
        attributeSpecification = new AttributeSpecification();
        this.useApi = useApi;
        this.channel = channel;
        this.retrievedClass = retrievedClass;
        this.nodeLabel = nodeLabel;
        this.retrievedClassAttributes = new TreeMap<>();
        this.retrievedClassAttributes.put(retrievedClass, classAttributes);
    }

    /**
     * Returns the channel this OutputManager is created from.
     * @return The channel this OutputManager is associated with.
     */
    public String getChannel() {
        return channel;
    }

    /**
     * Retrieves the objects of class <i>retrievedClass</i> from the currently displayed object diagram sorted in ascending order by the timestamp.
     * @return The list of objects available in the currently displayed object diagram.
     */
    public List<MObjectState> getUnprocessedModelObjects() {
        List<MObjectState> result = new ArrayList<>();
        for (String keyClass : retrievedClassAttributes.keySet()) {
            result.addAll(useApi.getObjectsOfClass(keyClass));
            result.sort((c1, c2) -> {
                if (useApi.getIntegerAttribute(c1, "timestamp") > useApi.getIntegerAttribute(c2, "timestamp"))
                    return 1;
                if (useApi.getIntegerAttribute(c1, "timestamp") > useApi.getIntegerAttribute(c2, "timestamp"))
                    return -1;
                return 0;
            });
            result.removeIf(obj -> useApi.getBooleanAttribute(obj, IS_PROCESSED));
            deleteProcessedObjects(useApi.getObjectsOfClass(keyClass));
        }

        return result;
    }

    public void deleteProcessedObjects(List<MObjectState> processedObjects) {
        //Delete all the processed
        for(MObjectState pr : processedObjects)
        {
            if(useApi.getBooleanAttribute(pr, IS_PROCESSED)) {
                // Clean up
                try {
                    cleanUpModel(pr);
                } catch (Exception ex) {
                    DTLogger.error(getChannel(), "Could not clean up model:");
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Saves all the objects in the currently displayed object diagram to the data lake.
     * @param session A Neo4j session object.
     */
    public void saveObjectsToDataLake(Session session, List<MObjectState> unprocessedObjects) {
        useApi.updateDerivedValues();
        for (MObjectState objstate : unprocessedObjects) {
            saveOneObject(session, objstate);
        }
    }

    /**
     * Auxiliary method to store the object in the database, extracted from the diagram.
     * @param objstate The object to store.
     */
    private synchronized void saveOneObject(Session session, MObjectState objstate) {
        useApi.updateDerivedValues();

        int timestamp = useApi.getIntegerAttribute(objstate, "timestamp");
        //String objectTypeAndId = nodeLabel + ":" + getObjectId(objstate);
        String objectTypeAndId = "DT" + objstate.object().cls().name() + ":" + getObjectId(objstate);
        AttributeSpecification keyAttributes = retrievedClassAttributes.get(objstate.object().cls().name());

        Map<String, Object> attributes = new HashMap<>();

        session.writeTransaction(tx -> {

            // Collect attributes
            for (String attr : keyAttributes.attributeNames()) {
                AttributeType attrType = keyAttributes.typeOf(attr);
                int multiplicity = keyAttributes.multiplicityOf(attr);
                String attrValue = useApi.getAttributeAsString(objstate, attr);
                if (attrValue != null) {
                    if (multiplicity > 1) {
                        // A sequence of values
                        Object[] values = extractValuesFromCollection(attrValue, attrType);
                        if (values.length == multiplicity) {
                            attributes.put(attr, values);
                        } else {
                            DTLogger.warn(getChannel(),
                                    "Error saving output object " + objectTypeAndId + ": "
                                            + "attribute " + attr + " has " + values.length
                                            + " value(s), but we need " + multiplicity);
                        }
                    } else {
                        // A single value
                        System.out.println("Valor del atributo: " + attrValue);
                        System.out.println("Nombre del atributo: " + attr);
                        Object result = attrType.fromUseToCypherObject(attrValue);
                        attributes.put(attr, result);
                    }
                } else {
                    DTLogger.warn(getChannel(),
                            "Error saving output object " + objectTypeAndId + ": "
                                    + "attribute " + attr + " not found in class " + objstate.getClass().getName());
                }
            }

            // Create the node
            Result newObject = tx.run("CREATE (o:" + "DT" + objstate.object().cls().name() + " $attributes) RETURN id(o)",
                    parameters("attributes", attributes));
            int nodeId = newObject.single().get("id(o)", 0);

            // Create relationships
            String twinId = useApi.getStringAttribute(objstate, "associatedThing.id");
            String executionId = useApi.getStringAttribute(objstate, "executionId");

            tx.run("MATCH (r:" + objstate.object().cls().name().split("State")[0] + "), (o:" + "DT" + objstate.object().cls().name() + ") " +
                            "WHERE r.twinId = $twinId AND r.executionId = $executionId " +
                            "AND id(o) = $id " +
                            "CREATE (r)-[:HAS_STATE]->(o)",
                    parameters(
                            "twinId", twinId,
                            "executionId", executionId,
                            "id", nodeId));

            DTNeo4jUtils.ensureTimestamp(tx, timestamp);

            tx.run("MATCH (o:" + "DT" + objstate.object().cls().name() + "), (t:Time) " +
                    "WHERE id(o) = $id AND t.timestamp = $timestamp " +
                    "CREATE (o)-[:AT_TIME]->(t)",
                    parameters(
                            "timestamp", timestamp,
                            "id", nodeId));

            createExtraRelationships(tx, nodeId, objstate);

            // Update timestamp
            DTNeo4jUtils.updateDTTimestampInDataLake(tx, timestamp);

            return null;
        });

        DTLogger.info(getChannel(), "Saved output object: " + objectTypeAndId);

        // Save the object
        int time = useApi.getCurrentTime();
        useApi.setAttribute(objstate, IS_PROCESSED, true);
        useApi.setAttribute(objstate, WHEN_PROCESSED, time);

        // Clean up
        try {
            cleanUpModel(objstate);
        } catch (Exception ex) {
            DTLogger.error(getChannel(), "Could not clean up model:");
            ex.printStackTrace();
        }
    }

    /**
     * Generates and returns an identifier for an object to be stored in the data lake.
     * @param objstate The object state to generate the identifier from.
     * @return The identifier for the object.
     */
    protected abstract String getObjectId(MObjectState objstate);

    /**
     * Override to implement connections of each node to create to other existing nodes.
     * @param tx A Neo4j transaction object.
     */
    protected abstract void createExtraRelationships(Transaction tx, int nodeId, MObjectState objstate);

    /**
     * Removes processed objects from the Data Lake.
     * @param objstate The object state that has been processed.
     */
    protected abstract void cleanUpModel(MObjectState objstate) throws UseApiException;

    /**
     * Converts an USE collection value to an array of values.
     * @param collection The collection value to convert.
     * @param baseType Type of each element in the collection.
     * @return An array of strings containing each value in the collection.
     */
    private Object[] extractValuesFromCollection(String collection, AttributeType baseType) {
        collection = collection.replaceAll("(?:Set|Bag|OrderedSet|Sequence)\\{(.*)}", "$1");
        String[] objects = collection.split(",");
        Object[] result = new Object[objects.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = baseType.fromUseToCypherObject(objects[i]);
        }
        return result;
    }

}
