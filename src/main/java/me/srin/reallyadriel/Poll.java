package me.srin.reallyadriel;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor(staticName = "make")
public class Poll {
    private static final String URL = "https://poll-nextjs-mocha.vercel.app/api/og?N=%d%s&topic=%s";
    private final List<Option> options = new ArrayList<>();
    private final String topic;

    public void addOption(Option option) {
        options.add(option);
    }
    public void incrementOption(int optionNumber) {
        options.get(optionNumber - 1).increment();
    }
    private String optionString() {
        StringBuilder builder = new StringBuilder();
        options.forEach(builder::append);
        return builder.toString();
    }

    @Override
    public String toString() {
        return URL.formatted(options.size(), optionString(), topic);
    }

    @Setter @Getter
    @RequiredArgsConstructor(staticName = "create")
    public static class Option {
        private final int optionNumber;
        private final String message;
        private int count = 0;

        void increment() {
            count++;
        }

        @Override
        public String toString() {
            return "&" + optionNumber + "=" + count + "," + message;
        }
    }
}
