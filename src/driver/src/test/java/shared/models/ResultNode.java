package shared.models;

import net.bytebuddy.utility.nullability.MaybeNull;

public interface ResultNode {
    String getName();
    @MaybeNull Object getValue();
}
