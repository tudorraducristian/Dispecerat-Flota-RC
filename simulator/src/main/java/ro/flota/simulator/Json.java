package ro.flota.simulator;

import tools.jackson.databind.json.JsonMapper;

/** Un singur JsonMapper pentru tot simulatorul. În Jackson 3 e imutabil, deci îl pot folosi mai multe fire. */
public final class Json {

    public static final JsonMapper MAPPER = JsonMapper.builder().build();

    private Json() {
    }
}
