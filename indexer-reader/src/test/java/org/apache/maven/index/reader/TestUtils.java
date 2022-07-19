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

import org.apache.maven.index.reader.Record.Type;

import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.apache.maven.index.reader.Utils.*;

/**
 * Helpers to transform records from one to another representation, and, some helpers for publishing using Guava.
 */
public final class TestUtils
{
  private TestUtils() {
    // nothing
  }

  private static final RecordCompactor RECORD_COMPACTOR = new RecordCompactor();

  private static final RecordExpander RECORD_EXPANDER = new RecordExpander();

  public static Function<Record, Map<String, String>> compactFunction = RECORD_COMPACTOR::apply;

  public static Function<Map<String, String>, Record> expandFunction = RECORD_EXPANDER::apply;

  /**
   * Helper method, that "decorates" the stream of records to be written out with "special" Maven Indexer records, so
   * all the caller is needed to provide {@link Iterable} or {@link Record}s <strong>to be</strong> on the index, with
   * record type {@link Type#ARTIFACT_ADD}. This method will create the output as "proper" Maven Indexer record
   * stream, by adding the {@link Type#DESCRIPTOR}, {@link Type#ROOT_GROUPS} and {@link Type#ALL_GROUPS} special
   * records.
   */
  public static Iterable<Record> decorate(final Iterable<Record> iterable,
                                          final String repoId)
  {
    final TreeSet<String> allGroupsSet = new TreeSet<>();
    final TreeSet<String> rootGroupsSet = new TreeSet<>();
    return Stream.concat(
            Stream.of( descriptor( repoId ) ),
            Stream.concat(
                    StreamSupport.stream( iterable.spliterator(), false ),
                    Stream.concat(
                            Stream.of( allGroups( allGroupsSet ) ),
                            // placeholder, will be recreated at the end with proper content
                            Stream.of( rootGroups( rootGroupsSet ) )
                    )
            )
            // placeholder, will be recreated at the end with proper content
    ).map( rec ->
    {
      if ( Type.DESCRIPTOR == rec.getType() )
      {
        return rec;
      }
      else if ( Type.ALL_GROUPS == rec.getType() )
      {
        return allGroups( allGroupsSet );
      }
      else if ( Type.ROOT_GROUPS == rec.getType() )
      {
        return rootGroups( rootGroupsSet );
      }
      else
      {
        final String groupId = rec.get( Record.GROUP_ID );
        if ( groupId != null )
        {
          allGroupsSet.add( groupId );
          rootGroupsSet.add( rootGroup( groupId ) );
        }
        return rec;
      }
    } ).collect( Collectors.toList() );
  }
}
