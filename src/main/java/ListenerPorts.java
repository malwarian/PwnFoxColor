final class ListenerPorts {
    private ListenerPorts() {
    }

    static int fromInterface(String listenerInterface) {
        if (listenerInterface == null) {
            return -1;
        }
        int separator = listenerInterface.lastIndexOf(':');
        if (separator < 0) {
            return -1;
        }
        try {
            int port = Integer.parseInt(listenerInterface.substring(separator + 1).trim());
            return port >= 1 && port <= 65535 ? port : -1;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}
