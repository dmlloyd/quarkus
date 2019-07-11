package io.quarkus.nativeimage.deployment.items;

import java.io.IOException;
import java.io.OutputStream;

import org.wildfly.common.Assert;
import org.wildfly.common.function.ExceptionConsumer;

import io.quarkus.builder.item.MultiBuildItem;

/**
 *
 */
// TODO: make this extend an abstract base resource build item
public final class NativeImageResourceBuildItem extends MultiBuildItem {
    private final String name;
    private final ExceptionConsumer<OutputStream, IOException> dataWriter;

    public NativeImageResourceBuildItem(final String name, final ExceptionConsumer<OutputStream, IOException> dataWriter) {
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("dataWriter", dataWriter);
        this.name = name;
        this.dataWriter = dataWriter;
    }

    public String getName() {
        return name;
    }

    public ExceptionConsumer<OutputStream, IOException> getDataWriter() {
        return dataWriter;
    }

    public void writeTo(OutputStream os) throws IOException {
        dataWriter.accept(os);
    }

}
