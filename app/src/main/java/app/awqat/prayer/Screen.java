package app.awqat.prayer;

import android.view.View;

/** One bottom-navigation destination. */
interface Screen {
    View build();
    default void onShow() {}
    default void onHide() {}
}
