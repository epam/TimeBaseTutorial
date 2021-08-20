package com.epam.deltix.samples.timebase.advanced.schema;


import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.SchemaChangeTask;
import com.epam.deltix.qsrv.hf.tickdb.schema.*;
import com.epam.deltix.samples.timebase.BarMessage;
import com.epam.deltix.timebase.messages.SchemaDataType;
import com.epam.deltix.timebase.messages.SchemaElement;
import com.epam.deltix.timebase.messages.SchemaType;


/**
 * Demonstrates how to extend the schema of the stream
 */
public class ExtendSchemaSample {

    public static final String STREAM_KEY = "barsEX.stream";


    private static final RecordClassDescriptor BAR_CLASS_SCHEMA;

    static {
        try {
            BAR_CLASS_SCHEMA = (RecordClassDescriptor) Introspector.introspectSingleClass(BarMessage.class);
        } catch (Introspector.IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }


    public static void createSampleStream(DXTickDB db) {
        DXTickStream stream = db.getStream(STREAM_KEY);

        if (stream != null)
            stream.delete();

        stream = db.createStream(
                STREAM_KEY,
                STREAM_KEY,
                "Description Line1\nLine 2\nLine 3",
                0
        );

        stream.setFixedType(BAR_CLASS_SCHEMA);
    }


    public static void readSchema(DXTickDB db) {
        DXTickStream stream = db.getStream(STREAM_KEY);
        RecordClassDescriptor classDescriptor = stream.getFixedType();

        printClassDescriptor(classDescriptor);
    }

    public static void printClassDescriptor(RecordClassDescriptor classDescriptor) {
        // If have parent class, then print it
        if (classDescriptor.getParent() != null)
            printClassDescriptor(classDescriptor.getParent());

        System.out.println("******************************************");
        System.out.println("Class name : " + classDescriptor.getName());
        System.out.println("Class title : " + classDescriptor.getTitle());
        System.out.println("******************************************\n");

        // Get all field from this class
        DataField[] dataFields = classDescriptor.getFields();

        for (DataField dataField : dataFields) {

            // print info about field
            System.out.println("Field name : " + dataField.getName());
            System.out.println("Field title : " + dataField.getTitle());

            if (dataField instanceof NonStaticDataField) {  // check for non-static field
                System.out.println("NonStatic field");
                NonStaticDataField nonStaticDataField = (NonStaticDataField) dataField;
                System.out.println("Relative to : " + nonStaticDataField.getRelativeTo());
            } else if (dataField instanceof StaticDataField) {  // check for static field
                System.out.println("Static field");
                StaticDataField staticDataField = (StaticDataField) dataField;
                System.out.println("Static value : " + staticDataField.getStaticValue());
            }

            // Get field type and print it
            DataType fieldType = dataField.getType();
            printFieldType(fieldType);

            System.out.println();
        }
    }


    public static void printFieldType(DataType fieldType) {
        System.out.println("Type: " + fieldType.getBaseName());
        System.out.println("Nullable: " + fieldType.isNullable());
        if (fieldType instanceof ClassDataType) {
            printClassDataType((ClassDataType) fieldType);
        } else if (fieldType instanceof ArrayDataType) {
            printArrayDataType((ArrayDataType) fieldType);
        } else if (fieldType instanceof EnumDataType) {
            printEnumDataType((EnumDataType) fieldType);
        } else {
            System.out.println("Encoding: " + fieldType.getEncoding());

        }
    }

    // Print class data type
    public static void printClassDataType(ClassDataType fieldType) {
        if (fieldType.isFixed()) {
            System.out.println("\r\nField have fixed type.");
            RecordClassDescriptor classDescriptor = fieldType.getFixedDescriptor();
            printClassDescriptor(classDescriptor);
        } else {
            System.out.println("Field have polymorphic types.");
            RecordClassDescriptor[] classDescriptors = fieldType.getDescriptors();
            for (RecordClassDescriptor classDescriptor : classDescriptors)
                printClassDescriptor(classDescriptor);
        }
    }

    // Print array data type
    public static void printArrayDataType(ArrayDataType fieldType) {
        System.out.println("Element type: ");
        DataType dataType = fieldType.getElementDataType();
        printFieldType(dataType);
    }

    // Print enum data type
    public static void printEnumDataType(EnumDataType fieldType) {
        EnumClassDescriptor enumClassDescriptor = fieldType.getDescriptor();
        EnumValue[] enumValues = enumClassDescriptor.getValues();

        for (EnumValue enumValue : enumValues) {
            System.out.println("Enum symbol : " + enumValue.symbol + " value : " + enumValue.value);
        }
    }

    @SchemaElement(title = "Extend Bar message sample class")
    public static class BarMessageEx extends BarMessage {

        protected int objectSize;

        protected String ownerName;

        protected String ownerIP;

        @SchemaElement(name = "objectSize")
        @SchemaType(
                dataType = SchemaDataType.INTEGER,
                encoding = "INT32"
        )
        public int getObjectSize () {
            return objectSize;
        }

        public void setObjectSize (int objectSize) {
            this.objectSize = objectSize;
        }

        public boolean hasObjectSize() {
            return this.objectSize != IntegerDataType.INT32_NULL;
        }

        public void nullifyObjectSize() {
            this.objectSize = IntegerDataType.INT32_NULL;
        }

        @SchemaElement(name = "ownerIP")
        @SchemaType(
                dataType = SchemaDataType.VARCHAR,
                encoding = "UTF8"
        )
        public String getOwnerIP () {
            return ownerIP;
        }

        public void setOwnerIP (String ownerIP) {
            this.ownerIP = ownerIP;
        }

        public boolean hasOwnerIP() {
            return this.ownerIP != null;
        }

        public void nullifyOwnerIP() {
            this.ownerIP = null;
        }

        @SchemaElement(name = "ownerName")
        @SchemaType(
                dataType = SchemaDataType.VARCHAR,
                encoding = "UTF8"
        )
        public String getOwnerName () {
            return ownerName;
        }

        public void setOwnerName (String ownerName) {
            this.ownerName = ownerName;
        }

        public boolean hasOwnerName() {
            return this.ownerName != null;
        }

        public void nullifyOwnerName() {
            this.ownerName = null;
        }
    }

    public static void extendSchema(DXTickDB db) {
        DXTickStream stream = db.getStream(STREAM_KEY);

        // Save current Bar message class description
        RecordClassDescriptor barMessageClassDescriptor = stream.getFixedType();

        final String            name = BarMessageEx.class.getName();

        // Create the fields from new BarExMessage
        final DataField []      fields = {
                new NonStaticDataField ("objectSize", "Object Size", new IntegerDataType (IntegerDataType.ENCODING_INT32, true)),
                new NonStaticDataField ("ownerIP", "Owner IP", new VarcharDataType("UTF8", true, true)),
                new NonStaticDataField ("ownerName", "Owner Name", new VarcharDataType("UTF8", true, true)),
        };

        // Create new Bar message class description
        RecordClassDescriptor extendBarClassDescriptor =  new RecordClassDescriptor (
                name,
                "Extend Bar message sample class", false,
                barMessageClassDescriptor,
                fields
        );

        RecordClassSet in = new RecordClassSet ();
        in.addContentClasses(stream.getFixedType());
        RecordClassSet out = new RecordClassSet ();
        out.addContentClasses(extendBarClassDescriptor);

        StreamMetaDataChange change = SchemaAnalyzer.DEFAULT.getChanges
                (in, MetaDataChange.ContentType.Fixed,  out, MetaDataChange.ContentType.Fixed);

        // Change stream schema
        stream.execute(new SchemaChangeTask(change));

        System.out.println("\r\n*********************************");
        System.out.println("*   schema changed              *");
        System.out.println("*********************************\r\n");

    }

    public static void main(String[] args) {
        if (args.length == 0)
            args = new String[]{"dxtick://localhost:8011"};

        DXTickDB db = TickDBFactory.createFromUrl(args[0]);

        db.open(false);

        try {
            createSampleStream(db);
            readSchema(db);
            extendSchema(db);
            readSchema(db);
        } finally {
            db.close();
        }
    }
}
