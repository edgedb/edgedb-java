package shared.models;

import net.bytebuddy.utility.nullability.MaybeNull;
import org.jetbrains.annotations.Nullable;

public class ResultNodeImpl implements ResultNode {
    private final String name;
    private final @MaybeNull Object value;

    public ResultNodeImpl(String name, @MaybeNull Object value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public @Nullable Object getValue() {
        return this.value;
    }
}
