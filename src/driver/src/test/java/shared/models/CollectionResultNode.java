package shared.models;

public class CollectionResultNode extends ResultNodeImpl {
    private final String elementType;

    public CollectionResultNode(String type, String elementType, Object value) {
        super(type, value);
        this.elementType = elementType;
    }

    public String getElementType() {
        return this.elementType;
    }
}
