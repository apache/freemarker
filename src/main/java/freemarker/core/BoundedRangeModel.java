/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.core;


/**
 * A range between two integers (maybe 0 long).
 */
final class BoundedRangeModel extends RangeModel {

    private final int step, size;
    private final boolean rightAdaptive;
    private final boolean affectedByStringSlicingBug;
    
    /**
     * @param inclusiveEnd Tells if the {@code end} index is part of the range. 
     * @param rightAdaptive Tells if the right end of the range adapts to the size of the sliced value, if otherwise
     *     it would be bigger than that. 
     */
    BoundedRangeModel(int begin, int end, boolean inclusiveEnd, boolean rightAdaptive) {
        super(begin);
        step = begin <= end ? 1 : -1;
        size = Math.abs(end - begin) + (inclusiveEnd ? 1 : 0);
        this.rightAdaptive = rightAdaptive;
        this.affectedByStringSlicingBug = inclusiveEnd;
    }

    public int size() {
        return size;
    }
    
    int getStep() {
        return step;
    }

    boolean isRightUnbounded() {
        return false;
    }

    boolean isRightAdaptive() {
        return rightAdaptive;
    }

    boolean isAffactedByStringSlicingBug() {
        return affectedByStringSlicingBug;
    }
    
}
