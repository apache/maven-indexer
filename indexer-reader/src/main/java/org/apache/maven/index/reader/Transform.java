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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeSet;

import org.apache.maven.index.reader.Record.EntryKey;
import org.apache.maven.index.reader.Record.Type;

/**
 * Helpers to transform records from one to another representation, and, some helpers for publishing.
 *
 * @since 5.1.2
 */
public final class Transform
{
  private Transform() {
    // nothing
  }

  /**
   * Transforming function.
   */
  public interface Function<I, O>
  {
    O apply(I rec);
  }

  /**
   * Applies {@link Function} to an {@link Iterable} on the fly.
   */
  public static <I, O> Iterable<O> transform(final Iterable<I> iterable, final Function<I, O> function) {
    return new Iterable<O>()
    {
      public Iterator<O> iterator() {
        return new TransformIterator<I, O>(iterable.iterator(), function);
      }
    };
  }

  /**
   * Helper method, that "decorates" the stream of records to be written out with "special" Maven Indexer records, so
   * all the caller is needed to provide {@link Iterable} or {@link Record}s <strong>to be</strong> on the index, with
   * record type {@link Record.Type#ARTIFACT_ADD}. This method will create the output as "proper" Maven Indexer record
   * streeam, by adding the {@link Type#DESCRIPTOR}, {@link Type#ROOT_GROUPS} and {@link Type#ALL_GROUPS} special
   * records.
   */
  public static Iterable<Map<String, String>> decorateAndTransform(final Iterable<Record> iterable,
                                                                   final String repoId)
  {
    final RecordCompactor recordCompactor = new RecordCompactor();
    final TreeSet<String> allGroups = new TreeSet<String>();
    final TreeSet<String> rootGroups = new TreeSet<String>();
    final ArrayList<Iterator<Record>> iterators = new ArrayList<Iterator<Record>>();
    iterators.add(Collections.singletonList(descriptor(repoId)).iterator());
    iterators.add(iterable.iterator());
    iterators.add(Collections.singletonList(allGroups(allGroups)).iterator());
    iterators.add(Collections.singletonList(rootGroups(rootGroups)).iterator());
    return transform(
        new Iterable<Record>()
        {
          public Iterator<Record> iterator() {
            return new ConcatIterator<Record>(iterators.iterator());
          }
        },
        new Function<Record, Map<String, String>>()
        {
          public Map<String, String> apply(final Record rec) {
            if (Type.DESCRIPTOR == rec.getType()) {
              return recordCompactor.apply(descriptor(repoId));
            }
            else if (Type.ALL_GROUPS == rec.getType()) {
              return recordCompactor.apply(allGroups(allGroups));
            }
            else if (Type.ROOT_GROUPS == rec.getType()) {
              return recordCompactor.apply(rootGroups(rootGroups));
            }
            else {
              final String groupId = rec.get(Record.GROUP_ID);
              if (groupId != null) {
                allGroups.add(groupId);
                rootGroups.add(Utils.rootGroup(groupId));
              }
              return recordCompactor.apply(rec);
            }
          }
        }
    );
  }

  private static Record descriptor(final String repoId) {
    HashMap<EntryKey, Object> entries = new HashMap<EntryKey, Object>();
    entries.put(Record.REPOSITORY_ID, repoId);
    return new Record(Type.DESCRIPTOR, entries);
  }

  private static Record allGroups(final Collection<String> allGroups) {
    HashMap<EntryKey, Object> entries = new HashMap<EntryKey, Object>();
    entries.put(Record.ALL_GROUPS, allGroups.toArray(new String[allGroups.size()]));
    return new Record(Type.ALL_GROUPS, entries);
  }

  private static Record rootGroups(final Collection<String> rootGroups) {
    HashMap<EntryKey, Object> entries = new HashMap<EntryKey, Object>();
    entries.put(Record.ROOT_GROUPS, rootGroups.toArray(new String[rootGroups.size()]));
    return new Record(Type.ROOT_GROUPS, entries);
  }

  // ==

  private static final class TransformIterator<I, O>
      implements Iterator<O>
  {
    private final Iterator<I> iterator;

    private final Function<I, O> function;

    private TransformIterator(final Iterator<I> iterator, final Function<I, O> function) {
      this.iterator = iterator;
      this.function = function;
    }

    public boolean hasNext() {
      return iterator.hasNext();
    }

    public O next() {
      return function.apply(iterator.next());
    }

    public void remove() {
      throw new UnsupportedOperationException("remove");
    }
  }

  private static final class ConcatIterator<T>
      implements Iterator<T>
  {
    private final Iterator<Iterator<T>> iterators;

    private Iterator<T> current;

    private T nextElement;

    private ConcatIterator(final Iterator<Iterator<T>> iterators) {
      this.iterators = iterators;
      this.nextElement = getNextElement();
    }

    public boolean hasNext() {
      return nextElement != null;
    }

    public T next() {
      if (nextElement == null) {
        throw new NoSuchElementException();
      }
      T result = nextElement;
      nextElement = getNextElement();
      return result;
    }

    public void remove() {
      throw new UnsupportedOperationException("remove");
    }

    protected T getNextElement() {
      if ((current == null || !current.hasNext()) && iterators.hasNext()) {
        current = iterators.next();
      }
      while (current != null && !current.hasNext()) {
        if (!iterators.hasNext()) {
          current = null;
          break;
        }
        current = iterators.next();
      }
      if (current != null) {
        return current.next();
      }
      else {
        return null;
      }
    }
  }
}
