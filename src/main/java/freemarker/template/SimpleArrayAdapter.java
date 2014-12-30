package freemarker.template;

import java.io.Serializable;
import java.lang.reflect.Array;

import freemarker.ext.util.WrapperTemplateModel;

/**
 * Adapts an {@code array} of a non-primitive elements to the corresponding {@link TemplateModel} interface(s), most
 * importantly to {@link TemplateHashModelEx}. If you aren't wrapping an already existing {@code array}, but build a
 * sequence specifically to be used from a template, also consider using {@link SimpleSequence} (see comparison there).
 * 
 * @see SimpleSequence
 * @see SimpleListAdapter
 * @see TemplateSequenceModel
 * 
 * @since 2.3.22
 */
public abstract class SimpleArrayAdapter extends WrappingTemplateModel implements TemplateSequenceModel,
        AdapterTemplateModel, WrapperTemplateModel, Serializable {

    /**
     * Factory method for creating new adapter instances.
     * 
     * @param array
     *            The array to adapt; can't be {@code null}. Must be an array. 
     * @param wrapper
     *            The {@link ObjectWrapper} used to wrap the items in the array. Has to be
     *            {@link ObjectWrapperAndUnwrapper} because of planned future features.
     */
    public static SimpleArrayAdapter adapt(Object array, ObjectWrapperAndUnwrapper wrapper) {
        final Class componentType = array.getClass().getComponentType();
        if (componentType == null) {
            throw new IllegalArgumentException("Not an array");
        }
        
        if (componentType.isPrimitive()) {
            if (componentType == int.class) {
                return new IntArrayAdapter((int[]) array, wrapper);
            }
            if (componentType == double.class) {
                return new DoubleArrayAdapter((double[]) array, wrapper);
            }
            if (componentType == long.class) {
                return new LongArrayAdapter((long[]) array, wrapper);
            }
            if (componentType == boolean.class) {
                return new BooleanArrayAdapter((boolean[]) array, wrapper);
            }
            if (componentType == float.class) {
                return new FloatArrayAdapter((float[]) array, wrapper);
            }
            if (componentType == char.class) {
                return new CharArrayAdapter((char[]) array, wrapper);
            }
            if (componentType == short.class) {
                return new ShortArrayAdapter((short[]) array, wrapper);
            }
            if (componentType == byte.class) {
                return new ByteArrayAdapter((byte[]) array, wrapper);
            }
            return new GenericPrimitiveArrayAdapter(array, wrapper);
        } else {
            return new ObjectArrayAdapter((Object[]) array, wrapper);
        }
    }

    private SimpleArrayAdapter(ObjectWrapper wrapper) {
        super(wrapper);
    }

    public final Object getAdaptedObject(Class hint) {
        return getWrappedObject();
    }

    private static class ObjectArrayAdapter extends SimpleArrayAdapter {

        private final Object[] array;

        private ObjectArrayAdapter(Object[] array, ObjectWrapper wrapper) {
            super(wrapper);
            this.array = array;
        }

        public TemplateModel get(int index) throws TemplateModelException {
            return index >= 0 && index < array.length ? wrap(array[index]) : null;
        }

        public int size() throws TemplateModelException {
            return array.length;
        }

        public Object getWrappedObject() {
            return array;
        }

    }

    private static class ByteArrayAdapter extends SimpleArrayAdapter {

        private final byte[] array;

        private ByteArrayAdapter(byte[] array, ObjectWrapper wrapper) {
            super(wrapper);
            this.array = array;
        }

        public TemplateModel get(int index) throws TemplateModelException {
            // Java 5: Use valueOf
            return index >= 0 && index < array.length ? wrap(new Byte(array[index])) : null;
        }

        public int size() throws TemplateModelException {
            return array.length;
        }

        public Object getWrappedObject() {
            return array;
        }

    }

    private static class ShortArrayAdapter extends SimpleArrayAdapter {

        private final short[] array;

        private ShortArrayAdapter(short[] array, ObjectWrapper wrapper) {
            super(wrapper);
            this.array = array;
        }

        public TemplateModel get(int index) throws TemplateModelException {
            // Java 5: Use valueOf
            return index >= 0 && index < array.length ? wrap(new Short(array[index])) : null;
        }

        public int size() throws TemplateModelException {
            return array.length;
        }

        public Object getWrappedObject() {
            return array;
        }

    }

    private static class IntArrayAdapter extends SimpleArrayAdapter {

        private final int[] array;

        private IntArrayAdapter(int[] array, ObjectWrapper wrapper) {
            super(wrapper);
            this.array = array;
        }

        public TemplateModel get(int index) throws TemplateModelException {
            // Java 5: Integer.valueOf
            return index >= 0 && index < array.length ? wrap(new Integer(array[index])) : null;
        }

        public int size() throws TemplateModelException {
            return array.length;
        }

        public Object getWrappedObject() {
            return array;
        }

    }

    private static class LongArrayAdapter extends SimpleArrayAdapter {

        private final long[] array;

        private LongArrayAdapter(long[] array, ObjectWrapper wrapper) {
            super(wrapper);
            this.array = array;
        }

        public TemplateModel get(int index) throws TemplateModelException {
            // Java 5: Use valueOf
            return index >= 0 && index < array.length ? wrap(new Long(array[index])) : null;
        }

        public int size() throws TemplateModelException {
            return array.length;
        }

        public Object getWrappedObject() {
            return array;
        }

    }

    private static class FloatArrayAdapter extends SimpleArrayAdapter {

        private final float[] array;

        private FloatArrayAdapter(float[] array, ObjectWrapper wrapper) {
            super(wrapper);
            this.array = array;
        }

        public TemplateModel get(int index) throws TemplateModelException {
            // Java 5: Use valueOf
            return index >= 0 && index < array.length ? wrap(new Float(array[index])) : null;
        }

        public int size() throws TemplateModelException {
            return array.length;
        }

        public Object getWrappedObject() {
            return array;
        }

    }

    private static class DoubleArrayAdapter extends SimpleArrayAdapter {

        private final double[] array;

        private DoubleArrayAdapter(double[] array, ObjectWrapper wrapper) {
            super(wrapper);
            this.array = array;
        }

        public TemplateModel get(int index) throws TemplateModelException {
            // Java 5: Use valueOf
            return index >= 0 && index < array.length ? wrap(new Double(array[index])) : null;
        }

        public int size() throws TemplateModelException {
            return array.length;
        }

        public Object getWrappedObject() {
            return array;
        }

    }

    private static class CharArrayAdapter extends SimpleArrayAdapter {

        private final char[] array;

        private CharArrayAdapter(char[] array, ObjectWrapper wrapper) {
            super(wrapper);
            this.array = array;
        }

        public TemplateModel get(int index) throws TemplateModelException {
            // Java 5: Use valueOf
            return index >= 0 && index < array.length ? wrap(new Character(array[index])) : null;
        }

        public int size() throws TemplateModelException {
            return array.length;
        }

        public Object getWrappedObject() {
            return array;
        }

    }

    private static class BooleanArrayAdapter extends SimpleArrayAdapter {

        private final boolean[] array;

        private BooleanArrayAdapter(boolean[] array, ObjectWrapper wrapper) {
            super(wrapper);
            this.array = array;
        }

        public TemplateModel get(int index) throws TemplateModelException {
            // Java 5: Use valueOf
            return index >= 0 && index < array.length ? wrap(new Boolean(array[index])) : null;
        }

        public int size() throws TemplateModelException {
            return array.length;
        }

        public Object getWrappedObject() {
            return array;
        }

    }

    /**
     * Much slower than the specialized versions; used only as the last resort.
     */
    private static class GenericPrimitiveArrayAdapter extends SimpleArrayAdapter {

        private final Object array;
        private final int length;

        private GenericPrimitiveArrayAdapter(Object array, ObjectWrapper wrapper) {
            super(wrapper);
            this.array = array;
            length = Array.getLength(array);
        }

        public TemplateModel get(int index) throws TemplateModelException {
            // Java 5: Integer.valueOf
            return index >= 0 && index < length ? wrap(Array.get(array, index)) : null;
        }

        public int size() throws TemplateModelException {
            return length;
        }

        public Object getWrappedObject() {
            return array;
        }

    }

}
