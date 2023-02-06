package digital.twin;

import org.neo4j.driver.Record;
import org.neo4j.driver.types.Node;
import org.tzi.use.uml.sys.MObjectState;
import services.Service;

import java.util.Objects;

/**
 * @author Paula Muñoz, Daniel Pérez - University of Málaga
 * InputManager that retrieves all Commands in the data lake and converts them to USE objects.
 */
public class CommandManager extends InputManager {

    public CommandManager(DTUseFacade useApi) {
        super(useApi, Service.COMMAND_IN_CHANNEL, "Command");
        attributeSpecification.set("commandOperation", AttributeType.STRING);
        //attributeSpecification.set("name", AttributeType.STRING);
        //attributeSpecification.set("arguments", AttributeType.STRING);
        //attributeSpecification.set("commandId", AttributeType.INTEGER);
    }
    @Override
    protected String getTargetClass(Record rec) {
        Node r = rec.get("r").asNode();
        return r.get("commandOperation").asString();
    }

    @Override
    protected String getObjectId(Record rec) {
        Node r = rec.get("r").asNode();
        return r.get("twinId") + ":" + rec.get("r.executionId") + ":" + rec.get("id(r)");
    }

    @Override
    protected String getOrdering() {
        return "id(r)";
    }

    protected void customSave(Node i, MObjectState objstate, Record rec) {
        Object value = useApi.getObjectsOfClass(i.get("twinClass").asString()).get(0);
        for (MObjectState o1 : useApi.getObjectsOfClass(i.get("twinClass").asString()))
        {
            //ToString adds double quotes when extracting String from Neo4J
            if(Objects.equals(useApi.getStringAttribute(o1, "id"), i.get("twinId").toString().replaceAll("\"", ""))) {
                value = o1;
            }
        }
        useApi.setAttribute(objstate, "deviceAssociated", value);
        useApi.setAttribute(objstate, "executionId", rec.get("r.executionId").asString());
        useApi.setAttribute(objstate, "isProcessed", true);
        useApi.setAttribute(objstate, "timestamp", useApi.getIntegerAttribute(useApi.getAnyObjectOfClass("Clock"), "now"));
        useApi.setAttribute(objstate, "whenProcessed", useApi.getIntegerAttribute(useApi.getAnyObjectOfClass("Clock"), "now"));

        if(i.containsKey("args")) {
            String args = i.get("args").asString();
            for(String arg : args.substring(1,args.length()-1).split(",")) {
                useApi.setAttribute(objstate, arg.split(":")[0], useApi.fromStringToObject(arg.split(":")[1], arg.split(":")[2]));
            }
        }
    }

}
