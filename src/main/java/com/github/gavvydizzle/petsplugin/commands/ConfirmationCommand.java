package com.github.gavvydizzle.petsplugin.commands;

public interface ConfirmationCommand {

    /**
     * @return The confirmation message that this command will send to the sender
     */
    String getConfirmationMessage();

}
