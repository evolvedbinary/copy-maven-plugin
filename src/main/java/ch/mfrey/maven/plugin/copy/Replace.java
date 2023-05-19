/*
 * Copyright © 2015 Martin Frey (mfrey.ch)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.mfrey.maven.plugin.copy;

import org.apache.maven.plugins.annotations.Parameter;

public class Replace {

    @Parameter(required = true)
    private String from;

    @Parameter(required = false, defaultValue = "false")
    private boolean regex;

    @Parameter(required = true)
    private String to;

    public Replace() {
        super();
    }

    public static Replace asRegex(final String from, final String to) {
        return new Replace().setFrom(from).setTo(to).setRegex(true);
    }

    public static Replace asReplace(final String from, final String to) {
        return new Replace().setFrom(from).setTo(to);
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public boolean isRegex() {
        return regex;
    }

    public Replace setFrom(final String from) {
        this.from = from;
        return this;
    }

    public Replace setRegex(final boolean regex) {
        this.regex = regex;
        return this;
    }

    public Replace setTo(final String to) {
        this.to = to;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Replace [from=").append(from).append(", to=").append(to).append(", regex=").append(regex)
                .append("]");
        return builder.toString();
    }

}