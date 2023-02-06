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
 * InputManager that retrieves all MovingObjects in the data lake and converts them to USE objects.
 */
public class MovingManager extends InputManager {

    public MovingManager(DTUseFacade useApi) {
        super(useApi, Service.MOVING_IN_CHANNEL, "MovingObject");
        //attributeSpecification.set("originPoint", AttributeType.STRING);
        attributeSpecification.set("length", AttributeType.REAL);
    }

    @Override
    protected String getTargetClass(Record rec) {
        setObjectName(rec.get("r").asNode().get("name").asString());
        return "MovingObject";
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
        TupleValue.Part x = new TupleValue.Part(0, "x", new RealValue(i.get("x").asDouble()));
        TupleValue.Part y = new TupleValue.Part(1, "y", new RealValue(i.get("y").asDouble()));
        TupleValue.Part z = new TupleValue.Part(2, "z", new RealValue(i.get("z").asDouble()));
        TupleType.Part xType = new TupleType.Part(0, "x", TypeFactory.mkReal());
        TupleType.Part yType = new TupleType.Part(1, "y", TypeFactory.mkReal());
        TupleType.Part zType = new TupleType.Part(2, "z", TypeFactory.mkReal());
        ArrayList<TupleValue.Part> positionTuple = new ArrayList<>();
        positionTuple.add(x);
        positionTuple.add(y);
        positionTuple.add(z);
        TupleType.Part[] positionTypes = {xType, yType, zType};
        useApi.setAttribute(objstate, "originPoint", new TupleValue(TypeFactory.mkTuple(positionTypes), positionTuple));
        useApi.setAttribute(objstate, "executionId", rec.get("r.executionId").asString());
        useApi.setAttribute(objstate, "isProcessed", true);
        useApi.setAttribute(objstate, "timestamp", useApi.getIntegerAttribute(useApi.getAnyObjectOfClass("Clock"), "now"));
        useApi.setAttribute(objstate, "whenProcessed", useApi.getIntegerAttribute(useApi.getAnyObjectOfClass("Clock"), "now"));
        useApi.setAttribute(objstate, "length", i.get("length").asDouble());
    }
}
