package utils;

import org.tzi.use.api.UseApiException;
import org.tzi.use.api.UseSystemApi;
import org.tzi.use.uml.mm.*;
import org.tzi.use.uml.ocl.expr.ExpObjRef;
import org.tzi.use.uml.ocl.expr.Expression;
import org.tzi.use.uml.ocl.expr.ExpressionWithValue;
import org.tzi.use.uml.ocl.type.Type;
import org.tzi.use.uml.ocl.value.*;
import org.tzi.use.uml.sys.*;
import org.tzi.use.uml.sys.soil.MObjectOperationCallStatement;
import org.tzi.use.uml.sys.soil.MStatement;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author -
 * A collection of utility methods for USE model manipulation.
 */
@SuppressWarnings("unused")
public class UseFacade {

    private static final Expression[] emptyArgs = new Expression[0];

    private final UseSystemApi api;
    private final ReentrantLock mutex;

    /**
     * Sets the API instance to use for all subsequent calls to UseFacade methods.
     * @param api The USE API instance to interact with the currently displayed object diagram.
     */
    public UseFacade(UseSystemApi api) {
        this.api = api;
        mutex = new ReentrantLock();
    }

    public void updateDerivedValues() {
        api.getSystem().state().updateDerivedValues(true);
    }

    // Object Creation and Destruction
    // ============================================================================================

    /**
     * Creates an object in the USE model. If the object exists, it updates it.
     * @param className The class of the new object.
     * @param objectName The name to assign to the object.
     * @return The state of the new object.
     * @throws MSystemException If some error occurs within the USE API.
     */
    public MObjectState createObject(String className, String objectName)
            throws MSystemException {
        try {
            mutex.lock();
            MClass mclass = api.getSystem().model().getClass(className);
            for(MObjectState o : getObjectsOfClass(className))
            {
                System.out.println("Objecto existente: " +o.object().name());
                System.out.println("Objecto a crear: " +objectName);
                if(o.object().name().equals(objectName)) return o;
            }
            MSystemState state = api.getSystem().state();
            return state.createObject(mclass, objectName).state(state);
        } finally {
            mutex.unlock();
        }
    }

    /**
     * Destroys an object in the USE model.
     * @param objstate The state of the object to destroy.
     * @throws UseApiException If some error occurs within the USE API.
     */
    public void destroyObject(MObjectState objstate) throws UseApiException {
        try {
            mutex.lock();
            api.deleteObjectEx(objstate.object());
        } finally {
            mutex.unlock();
        }
    }

    // Object Searching
    // ============================================================================================

    /**
     * Returns all objects of a specific class in the USE model.
     * @param className The name of the class whose instances to retrieve.
     * @return A list with all objects of the specified class.
     */
    public List<MObjectState> getObjectsOfClass(String className) {
        List<MObjectState> result = new ArrayList<>();
        MClass mclass = api.getSystem().model().getClass(className);
        try {
            mutex.lock();
            for (MObject o : api.getSystem().state().allObjects()) {
                if (o.cls().allSupertypes().contains(mclass)) {
                    MObjectState ostate = o.state(api.getSystem().state());
                    result.add(ostate);
                }
            }
            return result;
        } finally {
            mutex.unlock();
        }
    }

    /**
     * Returns any object of a specific class in the USE model.
     * @param className The name of the class whose instance to retrieve.
     * @return An instance of the given class, or null if no instances are found.
     */
    public MObjectState getAnyObjectOfClass(String className) {
        MObjectState result = null;
        MClass mclass = api.getSystem().model().getClass(className);
        try {
            mutex.lock();
            for (MObject o : api.getSystem().state().allObjects()) {
                if (o.cls().allSupertypes().contains(mclass)) {
                    result = (o.state(api.getSystem().state()));
                    break;
                }
            }
            return result;
        } finally {
            mutex.unlock();
        }
    }

    // Attribute Getters
    // ============================================================================================

    /**
     * Returns the value of an integer attribute in the model.
     * @param objstate The state of the object whose attribute to retrieve.
     * @param attributeName The name of the attribute to retrieve.
     * @return The value of the attribute, or null if the value is not an integer.
     */
    public Integer getIntegerAttribute(MObjectState objstate, String attributeName) {
        Value v = objstate.attributeValue(attributeName);
        return v instanceof IntegerValue ? ((IntegerValue) v).value() : null;
    }

    /**
     * Returns the value of a real number attribute in the model.
     * @param objstate The state of the object whose attribute to retrieve.
     * @param attributeName The name of the attribute to retrieve.
     * @return The value of the attribute, or null if the value is not a real number.
     */
    public Double getRealAttribute(MObjectState objstate, String attributeName) {
        Value v = objstate.attributeValue(attributeName);
        return v instanceof RealValue ? ((RealValue) v).value() : null;
    }

    /**
     * Returns the value of a string attribute in the model.
     * @param objstate The state of the object whose attribute to retrieve.
     * @param attributeName The name of the attribute to retrieve.
     * @return The value of the attribute, or null if the value is not a string.
     */
    public String getStringAttribute(MObjectState objstate, String attributeName) {
        Value v = null;
        if(attributeName.contains(".")) {
            for (MLink l : api.getSystem().state().allLinks()) {
                for(MObject o1 : l.linkedObjects()) {
                    if(o1.name().equals(objstate.object().name()) && l.association().roleNames().contains(attributeName.split("\\.")[0])) {
                        for (MObject o2 : l.linkedObjects()) {
                            if (!o2.name().contains("State")) {
                                v = o2.state(api.getSystem().state()).attributeValue(attributeName.split("\\.")[1]);
                            }
                        }
                    }
                }
            }
        }else
        {
            v = objstate.attributeValue(attributeName);
        }

        return v instanceof StringValue ? ((StringValue) v).value() : null;
    }

    /**
     * Returns the value of a boolean attribute in the model.
     * @param objstate The state of the object whose attribute to retrieve.
     * @param attributeName The name of the attribute to retrieve.
     * @return The value of the attribute, or null if the value is not a boolean value.
     */
    public Boolean getBooleanAttribute(MObjectState objstate, String attributeName) {
        Value v = objstate.attributeValue(attributeName);
        return v instanceof BooleanValue ? ((BooleanValue) v).value() : null;
    }

    /**
     * Returns the value of any attribute as a string.
     * @param objstate The state of the object whose attribute to retrieve.
     * @param attributeName The name of the attribute to retrieve.
     * @return The value of the attribute.
     */
    public String getAttributeAsString(MObjectState objstate, String attributeName) {
        if(attributeName.contains(".")) {
            for (MLink l : api.getSystem().state().allLinks()) {
                for(MObject o1 : l.linkedObjects()) {
                    if(o1.name().equals(objstate.object().name()) && l.association().roleNames().contains(attributeName.split("\\.")[0])) {
                        for (MObject o2 : l.linkedObjects()) {
                            if (!o2.name().contains("State")) {
                                return o2.state(api.getSystem().state()).attributeValue(attributeName.split("\\.")[1]).toString();
                            }
                        }
                    }
                }
            }
        }

        return objstate.attributeValue(attributeName).toString();
    }

    // Attribute Setters
    // ============================================================================================

    /**
     * Sets the value of attribute <i>attributeName</i>.
     * @param objstate The object whose attribute to set.
     * @param attributeName The name of the attribute to set.
     * @param value The value to set.
     */
    public void setAttribute(MObjectState objstate, String attributeName, Object value) {
        if(value instanceof TupleValue) {
            setAttributeAux(objstate, attributeName, (TupleValue) value);
        }else {
            setAttributeAux(objstate, attributeName, objectToUseValue(value));
        }
    }

    @SuppressWarnings("unchecked")
    private Value objectToUseValue(Object object) {
        if (object instanceof Integer) {
            return IntegerValue.valueOf((int) object);
        } else if (object instanceof Long) {
            return IntegerValue.valueOf(Math.toIntExact((long) object));
        } else if (object instanceof Double) {
            return new RealValue((double) object);
        } else if (object instanceof Float) {
            return new RealValue((float) object);
        } else if (object instanceof String) {
            return new StringValue((String) object);
        } else if (object instanceof Boolean) {
            return BooleanValue.get((boolean) object);
        } else if (object instanceof MObjectState) {
            return new ObjectValue(((MObjectState) object).object().cls(), ((MObjectState) object).object());
        } else if (object instanceof List) {
            List<Object> list = (List<Object>) object;
            Type type = null;
            boolean multipleTypes = false;
            List<Value> useValues = new ArrayList<>();
            for (Object listobj : list) {
                Value newValue = objectToUseValue(listobj);
                if (type != null) {
                    if (!type.equals(newValue.type())) {
                        multipleTypes = true;
                        break;
                    }
                } else {
                    type = newValue.type();
                }
                useValues.add(newValue);
            }
            if (type != null && !multipleTypes) {
                return new SequenceValue(type, useValues);
            } else {
                throw new RuntimeException("Cannot convert this Java object to a USE value");
            }
        } else {
            throw new RuntimeException("Cannot convert this Java object to a USE value");
        }
    }

    private void setAttributeAux(MObjectState objstate, String attributeName, Value value) {
        MClass mclass = objstate.object().cls();
        MAttribute attribute = mclass.attribute(attributeName, true);
        try {
            mutex.lock();
            objstate.setAttributeValue(attribute, value);
        } finally {
            mutex.unlock();
        }
    }

    public Object fromStringToObject(String value, String type) {
        switch (type) {

            case "BOOLEAN":
                return Boolean.parseBoolean(value);

            case "INTEGER":
                return Integer.parseInt(value);

            case "REAL":
                return Double.parseDouble(value);

            default:
                return value;

        }
    }

    // Operation calls
    // ============================================================================================

    /**
     * Calls an operation without passing arguments.
     * @param objstate The object state whose operation to call.
     * @param operationName The name of the operation to call.
     * @return A StatementEvaluationResult object.
     * @throws MSystemException If some error occurs within the USE API.
     */
    public StatementEvaluationResult callOperation(
            MObjectState objstate, String operationName) throws MSystemException {
        return callOperation(objstate, operationName, (Object) null);
    }

    /**
     * Calls an operation
     * @param objstate The object state whose operation to call.
     * @param operationName The name of the operation to call.
     * @param args Arguments to pass to the operation.
     * @return A StatementEvaluationResult object.
     * @throws MSystemException If some error occurs within the USE API.
     */
    public StatementEvaluationResult callOperation(
            MObjectState objstate, String operationName, Object... args) throws MSystemException {
        MObject mobject = objstate.object();
        MClass mclass = mobject.cls();
        MOperation operation = mclass.operation(operationName, true);

        // Convert arguments to USE expressions
        Expression[] useArgs = emptyArgs;
        if (args != null) {
            useArgs = new Expression[args.length];
            for (int i = 0; i < args.length; i++) {
                useArgs[i] = new ExpressionWithValue(objectToUseValue(args[i]));
            }
        }

        // Create and execute statement
        MStatement stmt = new MObjectOperationCallStatement(
                new ExpObjRef(mobject), operation, useArgs);
        try {
            mutex.lock();
            return api.getSystem().execute(stmt);
        } finally {
            mutex.unlock();
        }
    }

}
