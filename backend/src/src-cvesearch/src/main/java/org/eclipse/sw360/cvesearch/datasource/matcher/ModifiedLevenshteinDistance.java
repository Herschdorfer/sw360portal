/*
 * Copyright (c) Bosch Software Innovations GmbH 2016.
 * Part of the SW360 Portal Project.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.cvesearch.datasource.matcher;

import java.util.Optional;

import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyString;

public class ModifiedLevenshteinDistance {

    public static Match levenshteinMatch(String needle, String haystack){
        return new Match(needle,
                calculateModifiedLevenshteinDistance(needle, nullToEmptyString(haystack).replace(' ', '_')));
    }

    public static class LevenshteinCost {

        private final int d;
        private final boolean consumed;

        public LevenshteinCost(int d){
            this.d = d;
            this.consumed = false;
        }

        private LevenshteinCost(int d, boolean consumed){
            this.d = d;
            this.consumed = consumed;
        }

        public int getD() {
            return d;
        }

        public boolean hasConsumed() {
            return consumed;
        }

        public LevenshteinCost increment() {
            return new LevenshteinCost(d + 1, consumed);
        }

        public LevenshteinCost incrementOrConsume(boolean hasConsumed) {
            if(hasConsumed){
                return new LevenshteinCost(d, true);
            }else{
                return this.increment();
            }
        }

        public LevenshteinCost merge(LevenshteinCost le){
            if(d > le.getD()){
                return le;
            }
            if(d == le.getD()){
                return new LevenshteinCost(d, consumed || le.hasConsumed());
            }
            return this;
        }
    }

    /**
     * This is a modified Levenshtein distance in which
     * - skipping prefixes and postfixes of the haystack does not cost anything
     * - if one of the strings is empty the distance Integer.MAX_VALUE is returned
     *
     * @param needle
     * @param haystack
     * @return the modified Levenshtein distance between the needle and the haystack
     */
    public static int calculateModifiedLevenshteinDistance(String needle, String haystack) {
        return calculateModifiedLevenshteinDistance(needle, haystack,'_');
    }

    /**
     * This is a modified Levenshtein distance in which
     * - skipping prefixes and postfixes of the haystack does not cost anything
     * - if one of the strings is empty the distance Integer.MAX_VALUE is returned
     *
     * @param needle
     * @param haystack
     * @param space the chosen representation of the separator
     * @return the modified Levenshtein distance between the needle and the haystack
     */
    public static int calculateModifiedLevenshteinDistance(String needle, String haystack, char space){

        if (needle.length() == 0 || haystack.length() == 0){
            return Integer.MAX_VALUE;
        }

        int needleLength = needle.length() + 1;
        int haystackLength = haystack.length() + 1;

        LevenshteinCost[] oldcost = new LevenshteinCost[needleLength];
        LevenshteinCost[] curcost = new LevenshteinCost[needleLength];

        for (int i = 0; i < needleLength; i++) oldcost[i] = new LevenshteinCost(i);

        int savedCostsWhenSkippedSpaceSeparatedPrefix                 = 0;
        LevenshteinCost minimalCostsWhenSkippedSpaceSeperatedPostfix = new LevenshteinCost(Integer.MAX_VALUE);
        for (int j = 1; j < haystackLength; j++) {
            //=========================================================================================================
            if (j > 0 && haystack.charAt(j - 1) == space) {
                // skipping prefix of haystack does not cost anything, if it ends with a space
                savedCostsWhenSkippedSpaceSeparatedPrefix = j;
            }
            curcost[0] = new LevenshteinCost(j - savedCostsWhenSkippedSpaceSeparatedPrefix);

            //=========================================================================================================
            for(int i = 1; i < needleLength; i++) {
                boolean charsMatch = Character.toLowerCase(needle.charAt(i - 1)) == Character.toLowerCase(haystack.charAt(j - 1));
                LevenshteinCost costReplace = oldcost[i - 1].incrementOrConsume(charsMatch);
                LevenshteinCost costInsert  = oldcost[i].increment();
                LevenshteinCost costDelete  = curcost[i - 1].increment();

                curcost[i] = costReplace.merge(costInsert).merge(costDelete);
            }

            //=========================================================================================================
            if(haystack.charAt(j - 1) == space) {
                // skipping postfix of haystack does not cost anything, if it starts with a space
                minimalCostsWhenSkippedSpaceSeperatedPostfix =
                        minimalCostsWhenSkippedSpaceSeperatedPostfix.merge(oldcost[needleLength - 1]);
            }

            //=========================================================================================================
            LevenshteinCost[] swap = oldcost; oldcost = curcost; curcost = swap;
        }

        LevenshteinCost finalCost = oldcost[needleLength - 1]
                .merge(minimalCostsWhenSkippedSpaceSeperatedPostfix);

        if(finalCost.hasConsumed()){
            return finalCost.getD();
        }else{
            return Integer.MAX_VALUE;
        }
    }
}
