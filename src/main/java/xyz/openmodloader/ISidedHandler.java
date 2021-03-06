package xyz.openmodloader;

import net.minecraft.util.text.ITextComponent;
import xyz.openmodloader.event.strippable.Side;

public interface ISidedHandler {
    void onInitialize();

    Side getSide();

    void openSnackbar(ITextComponent component);
}
