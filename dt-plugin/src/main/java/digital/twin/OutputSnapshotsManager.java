package digital.twin;

import org.neo4j.driver.Transaction;
import org.tzi.use.api.UseApiException;
import org.tzi.use.uml.sys.MObjectState;
import services.Service;

/**
 * @author Paula Muñoz, Daniel Pérez - University of Málaga
 * OutputManager that retrieves all OutputSnapshot instances and serializes them for storage in the data lake.
 */
public class OutputSnapshotsManager extends OutputManager {

    //private static final int NUMBER_OF_SERVOS = 6;

    /**
     * Default constructor.
     * @param useApi USE API facade instance to interact with the currently displayed object diagram.
     */
    public OutputSnapshotsManager(DTUseFacade useApi) {
        //super(useApi, Service.DT_OUT_CHANNEL, "OutputBraccioSnapshot", "OutputSnapshot", "IS_IN_STATE");
        super(useApi, Service.DT_OUT_CHANNEL, "LightState", "DTLightState", new AttributeSpecification());
        AttributeSpecification lightAttributes = retrievedClassAttributes.get("LightState");
        lightAttributes.set("executionId", AttributeType.STRING);
        lightAttributes.set("bri", AttributeType.INTEGER);
        lightAttributes.set("colormode", AttributeType.STRING);
        lightAttributes.set("alert", AttributeType.STRING);
        lightAttributes.set("hue", AttributeType.INTEGER);
        lightAttributes.set("on", AttributeType.BOOLEAN);
        lightAttributes.set("ct", AttributeType.INTEGER);
        lightAttributes.set("sat", AttributeType.INTEGER);
        lightAttributes.set("illumination", AttributeType.REAL);
        lightAttributes.set("associatedThing.id", AttributeType.STRING);

        AttributeSpecification movementAttributes = new AttributeSpecification();
        movementAttributes.set("executionId", AttributeType.STRING);
        movementAttributes.set("movement", AttributeType.BOOLEAN);
        movementAttributes.set("associatedThing.id", AttributeType.STRING);
        retrievedClassAttributes.put("MovementSensorState", movementAttributes);

        AttributeSpecification blindAttributes = new AttributeSpecification();
        blindAttributes.set("executionId", AttributeType.STRING);
        blindAttributes.set("openPercent", AttributeType.INTEGER);
        blindAttributes.set("associatedThing.id", AttributeType.STRING);
        retrievedClassAttributes.put("BlindState", blindAttributes);
    }

    @Override
    protected String getObjectId(MObjectState objstate) {
        String twinId = useApi.getStringAttribute(objstate, "associatedThing.id");
        String executionId = useApi.getStringAttribute(objstate, "executionId");
        Integer timestamp = useApi.getIntegerAttribute(objstate, "timestamp");
        assert twinId != null;
        assert executionId != null;
        assert timestamp != null;
        return twinId + ":" + executionId + ":" + timestamp;
    }

    @Override
    protected void createExtraRelationships(Transaction tx, int nodeId, MObjectState objstate) { }

    /**
     * Keep alive the last State in the Model
     * @param objstate The object state that has been processed.
     * @throws UseApiException
     */
    protected void cleanUpModel(MObjectState objstate) throws UseApiException {
        if (useApi.getObjectsOfClass(objstate.object().cls().name()).stream().filter(obj -> useApi.getStringAttribute(obj,"associatedThing.id") == useApi.getStringAttribute(objstate, "associatedThing.id")).count() > 1) {
            useApi.destroyObject(objstate);
        }
    }

}
