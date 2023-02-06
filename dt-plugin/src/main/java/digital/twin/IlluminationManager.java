package digital.twin;

import org.neo4j.driver.Record;
import org.neo4j.driver.types.Node;
import org.tzi.use.uml.ocl.type.TupleType;
import org.tzi.use.uml.ocl.type.TypeFactory;
import org.tzi.use.uml.ocl.value.RealValue;
import org.tzi.use.uml.ocl.value.TupleValue;
import org.tzi.use.uml.sys.MObjectState;
import services.Service;

import java.util.ArrayList;

/**
 * @author Juan Alberto Llopis - University of Almería
 * InputManager that retrieves all Illumination objects in the data lake and converts them to USE objects.
 */
public class IlluminationManager extends InputManager {

    public IlluminationManager(DTUseFacade useApi) {
        super(useApi, Service.ILLUMINATION_IN_CHANNEL, "Illumination");
        attributeSpecification.set("value", AttributeType.REAL);
    }

    @Override
    protected String getTargetClass(Record rec) {
        setObjectName(rec.get("r").asNode().get("name").asString());
        return "Illumination";
    }

    @Override
    protected String getObjectId(Record rec) {
        return rec.get("r.executionId") + ":" + rec.get("id(r)");
    }

    @Override
    protected String getOrdering() {
        return "id(r)";
    }

    protected void customSave(Node i, MObjectState objstate, Record rec) {
        useApi.setAttribute(objstate, "value", i.get("value").asDouble());
        useApi.setAttribute(objstate, "executionId", rec.get("r.executionId").asString());
        useApi.setAttribute(objstate, "isProcessed", true);
        useApi.setAttribute(objstate, "timestamp", useApi.getIntegerAttribute(useApi.getAnyObjectOfClass("Clock"), "now"));
        useApi.setAttribute(objstate, "whenProcessed", useApi.getIntegerAttribute(useApi.getAnyObjectOfClass("Clock"), "now"));
    }
}
