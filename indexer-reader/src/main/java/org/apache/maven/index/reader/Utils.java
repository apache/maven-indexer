package org.apache.maven.index.reader;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.TimeZone;
import java.util.regex.Pattern;

/**
 * Reusable code snippets and constants.
 *
 * @since 5.1.2
 */
public final class Utils
{
  private Utils() {
    // nothing
  }

  static final String INDEX_FILE_PREFIX = "nexus-maven-repository-index";

  static final DateFormat INDEX_DATE_FORMAT;

  static {
    INDEX_DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss.SSS Z");
    INDEX_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
  }

  static final String FIELD_SEPARATOR = "|";

  static final String NOT_AVAILABLE = "NA";

  static final String UINFO = "u";

  static final String INFO = "i";

  static final Pattern FS_PATTERN = Pattern.compile(Pattern.quote(FIELD_SEPARATOR));

  /**
   * Creates and loads {@link Properties} from provided {@link InputStream} and closes the stream.
   */
  static Properties loadProperties(final InputStream inputStream) throws IOException {
    try {
      final Properties properties = new Properties();
      properties.load(inputStream);
      return properties;
    }
    finally {
      inputStream.close();
    }
  }

  /**
   * Helper to translate the "NA" (not available) input into {@code null} value.
   */
  static String renvl(final String v) {
    return NOT_AVAILABLE.equals(v) ? null : v;
  }

  /**
   * Helper to translate {@code null} into "NA" (not available) value.
   */
  static String nvl(final String v) {
    return v == null ? NOT_AVAILABLE : v;
  }

  /**
   * Returns the "root group" of given groupId.
   */
  static String rootGroup(final String groupId) {
    int n = groupId.indexOf('.');
    if (n > -1) {
      return groupId.substring(0, n);
    }
    return groupId;
  }
}
