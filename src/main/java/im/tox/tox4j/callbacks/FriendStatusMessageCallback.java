package im.tox.tox4j.callbacks;

import im.tox.tox4j.annotations.NotNull;

public interface FriendStatusMessageCallback {

    void friendStatusMessage(int friendNumber, @NotNull byte[] message);

}
