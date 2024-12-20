package app.revanced.extension.youtube.sponsorblock.ui;

import android.view.View;
import android.widget.ImageView;

import java.lang.ref.WeakReference;
import java.util.Objects;

import app.revanced.extension.youtube.patches.VideoInformation;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.sponsorblock.SegmentPlaybackController;
import app.revanced.extension.youtube.sponsorblock.SponsorBlockUtils;
import app.revanced.extension.shared.Logger;
import app.revanced.extension.shared.Utils;
import app.revanced.extension.youtube.videoplayer.PlayerControlButton;

// Edit: This should be a subclass of PlayerControlButton
public class VotingButtonController {
    private static WeakReference<ImageView> buttonReference = new WeakReference<>(null);
    private static boolean isShowing;

    /**
     * injection point
     */
    public static void initialize(View youtubeControlsLayout) {
        try {
            Logger.printDebug(() -> "initializing voting button");
            ImageView imageView = Objects.requireNonNull(Utils.getChildViewByResourceName(
                    youtubeControlsLayout, "revanced_sb_voting_button"));
            imageView.setVisibility(View.GONE);
            imageView.setOnClickListener(v -> SponsorBlockUtils.onVotingClicked(v.getContext()));

            buttonReference = new WeakReference<>(imageView);
        } catch (Exception ex) {
            Logger.printException(() -> "initialize failure", ex);
        }
    }

    /**
     * injection point
     */
    public static void changeVisibilityImmediate(boolean visible) {
        if (visible) {
            // Fix button flickering, by pushing this call to the back of
            // the main thread and letting other layout code run first.
            Utils.runOnMainThread(() -> setVisibility(true, false));
        } else {
            setVisibility(false, false);
        }
    }

    /**
     * injection point
     */
    public static void changeVisibility(boolean visible, boolean animated) {
        // Ignore this call, otherwise with full screen thumbnails the buttons are visible while seeking.
        if (visible && !animated) return;

        setVisibility(visible, animated);
    }

    /**
     * injection point
     */
    private static void setVisibility(boolean visible, boolean animated) {
        try {
            if (isShowing == visible) return;
            isShowing = visible;

            ImageView iView = buttonReference.get();
            if (iView == null) return;

            if (visible) {
                iView.clearAnimation();
                if (!shouldBeShown()) {
                    return;
                }
                if (animated) {
                    iView.startAnimation(PlayerControlButton.getButtonFadeIn());
                }
                iView.setVisibility(View.VISIBLE);
                return;
            }

            if (iView.getVisibility() == View.VISIBLE) {
                iView.clearAnimation();
                if (animated) {
                    iView.startAnimation(PlayerControlButton.getButtonFadeOut());
                }
                iView.setVisibility(View.GONE);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "changeVisibility failure", ex);
        }
    }

    private static boolean shouldBeShown() {
        return Settings.SB_ENABLED.get() && Settings.SB_VOTING_BUTTON.get()
                && SegmentPlaybackController.videoHasSegments() && !VideoInformation.isAtEndOfVideo();
    }

    public static void hide() {
        if (!isShowing) {
            return;
        }
        Utils.verifyOnMainThread();
        View v = buttonReference.get();
        if (v == null) {
            return;
        }
        v.setVisibility(View.GONE);
        isShowing = false;
    }
}
