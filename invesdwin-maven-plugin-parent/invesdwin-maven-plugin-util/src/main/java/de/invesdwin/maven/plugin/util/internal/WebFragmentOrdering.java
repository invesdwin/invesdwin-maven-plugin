package de.invesdwin.maven.plugin.util.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.invesdwin.maven.plugin.util.WebFragmentResource;

/**
 * Modified from org.eclipse.jetty.webapp.Ordering.RelativeOrdering Version 9.0.6.
 */
//@NotThreadSafe
public class WebFragmentOrdering {
    protected List<WebFragmentResource> beforeOthers = new LinkedList<WebFragmentResource>();
    protected List<WebFragmentResource> afterOthers = new LinkedList<WebFragmentResource>();
    protected List<WebFragmentResource> noOthers = new LinkedList<WebFragmentResource>();

    /**
     * Order the list of jars according to the ordering declared in the various web-fragment.xml files.
     * 
     * @see org.eclipse.jetty.webapp.Ordering#order(java.util.List)
     */
    public List<WebFragmentResource> order(final List<WebFragmentResource> resources) {
        //for each jar, put it into the ordering according to the fragment ordering
        for (final WebFragmentResource r : resources) {
            //check if the jar has a fragment descriptor
            final WebFragmentOrderType orderType = r.getWebFragmentOrderType();
            switch (orderType) {
            case None: {
                addNoOthers(r);
                break;
            }
            case Before: {
                addBeforeOthers(r);
                break;
            }
            case After: {
                addAfterOthers(r);
                break;
            }
            default:
                throw new IllegalArgumentException(WebFragmentOrderType.class.getSimpleName() + " unknown: "
                        + orderType);
            }
        }

        //now apply the ordering
        final List<WebFragmentResource> orderedList = new ArrayList<WebFragmentResource>();
        int maxIterations = 2;
        boolean done = false;
        do {
            //1. order the before-others according to any explicit before/after relationships
            final boolean changesBefore = orderList(beforeOthers);

            //2. order the after-others according to any explicit before/after relationships
            final boolean changesAfter = orderList(afterOthers);

            //3. order the no-others according to their explicit before/after relationships
            final boolean changesNone = orderList(noOthers);

            //we're finished on a clean pass through with no ordering changes
            done = (!changesBefore && !changesAfter && !changesNone);
        } while (!done && (--maxIterations > 0));

        //4. merge before-others + no-others +after-others
        if (!done) {
            throw new IllegalStateException("Circular references for fragments");
        }

        for (final WebFragmentResource r : beforeOthers) {
            orderedList.add(r);
        }
        for (final WebFragmentResource r : noOthers) {
            orderedList.add(r);
        }
        for (final WebFragmentResource r : afterOthers) {
            orderedList.add(r);
        }

        return orderedList;
    }

    private void addBeforeOthers(final WebFragmentResource r) {
        beforeOthers.add(r);
    }

    private void addAfterOthers(final WebFragmentResource r) {
        afterOthers.add(r);
    }

    private void addNoOthers(final WebFragmentResource r) {
        noOthers.add(r);
    }

    private boolean orderList(final List<WebFragmentResource> list) {
        //Take a copy of the list so we can iterate over it and at the same time do random insertions
        boolean changes = false;
        final List<WebFragmentResource> iterable = new ArrayList<WebFragmentResource>(list);
        final Iterator<WebFragmentResource> itor = iterable.iterator();

        while (itor.hasNext()) {
            final WebFragmentResource r = itor.next();

            //Handle any explicit <before> relationships for the fragment we're considering
            final List<String> befores = r.getWebFragmentBefores();
            if (befores != null && !befores.isEmpty()) {
                for (final String b : befores) {
                    //Fragment we're considering must be before b
                    //Check that we are already before it, if not, move us so that we are.
                    //If the name does not exist in our list, then get it out of the no-other list
                    if (!isBefore(list, r.getFirstWebFragmentName(), b)) {
                        //b is not already before name, move it so that it is
                        final int idx1 = getIndexOf(list, r.getFirstWebFragmentName());
                        final int idx2 = getIndexOf(list, b);

                        //if b is not in the same list
                        if (idx2 < 0) {
                            changes = true;
                            // must be in the noOthers list or it would have been an error
                            final WebFragmentResource bResource = getResourceForFragment(list, b);
                            //If its in the no-others list, insert into this list so that we are before it
                            if (bResource != null && noOthers.remove(bResource)) {
                                insert(list, idx1 + 1, b);
                            }
                        } else {
                            //b is in the same list but b is before name, so swap it around
                            list.remove(idx1);
                            insert(list, idx2, r.getFirstWebFragmentName());
                            changes = true;
                        }
                    }
                }
            }

            //Handle any explicit <after> relationships
            final List<String> afters = r.getWebFragmentAfters();
            if (afters != null && !afters.isEmpty()) {
                for (final String a : afters) {
                    //Check that fragment we're considering is after a, moving it if possible if its not
                    if (!isAfter(list, r.getFirstWebFragmentName(), a)) {
                        //name is not after a, move it
                        final int idx1 = getIndexOf(list, r.getFirstWebFragmentName());
                        final int idx2 = getIndexOf(list, a);

                        //if a is not in the same list as name
                        if (idx2 < 0) {
                            changes = true;
                            //take it out of the noOthers list and put it in the right place in this list
                            final WebFragmentResource aResource = getResourceForFragment(list, a);
                            if (aResource != null && noOthers.remove(aResource)) {
                                insert(list, idx1, aResource);
                            }
                        } else {
                            //a is in the same list as name, but in the wrong place, so move it
                            list.remove(idx2);
                            insert(list, idx1, a);
                            changes = true;
                        }
                    }
                    //Name we're considering must be after this name
                    //Check we're already after it, if not, move us so that we are.
                    //If the name does not exist in our list, then get it out of the no-other list
                }
            }
        }

        return changes;
    }

    private WebFragmentResource getResourceForFragment(final List<WebFragmentResource> list, final String fragName) {
        for (final WebFragmentResource r : list) {
            if (r.getFirstWebFragmentName().equals(fragName)) {
                return r;
            }
        }
        return null;
    }

    /**
     * Is fragment with name a before fragment with name b?
     * 
     * @param list
     * @param fragNameA
     * @param fragNameB
     * @return true if fragment name A is before fragment name B
     */
    private boolean isBefore(final List<WebFragmentResource> list, final String fragNameA, final String fragNameB) {
        //check if a and b are already in the same list, and b is already
        //before a
        final int idxa = getIndexOf(list, fragNameA);
        final int idxb = getIndexOf(list, fragNameB);

        if (idxb >= 0 && idxb < idxa) {
            //a and b are in the same list but a is not before b
            return false;
        }

        if (idxb < 0) {
            //a and b are not in the same list, but it is still possible that a is before
            //b, depending on which list we're examining
            if (list == beforeOthers) {
                //The list we're looking at is the beforeOthers.If b is in the _afterOthers or the _noOthers, then by
                //definition a is before it
                return true;
            } else if (list == afterOthers) {
                //The list we're looking at is the afterOthers, then a will be the tail of
                //the final list.  If b is in the beforeOthers list, then b will be before a and an error.
                final WebFragmentResource bResource = getResourceForFragment(list, fragNameB);
                if (beforeOthers.contains(bResource)) {
                    throw new IllegalStateException("Incorrect relationship: " + fragNameA + " before " + fragNameB);
                } else {
                    return false; //b could be moved to the list
                }
            }
        }

        //a and b are in the same list and a is already before b
        return true;
    }

    /**
     * Is fragment name "a" after fragment name "b"?
     * 
     * @param list
     * @param fragNameA
     * @param fragNameB
     * @return true if fragment name A is after fragment name B
     */
    private boolean isAfter(final List<WebFragmentResource> list, final String fragNameA, final String fragNameB) {
        final int idxa = getIndexOf(list, fragNameA);
        final int idxb = getIndexOf(list, fragNameB);

        if (idxb >= 0 && idxa < idxb) {
            //a and b are both in the same list, but a is before b
            return false;
        }

        if (idxb < 0) {
            //a and b are in different lists. a could still be after b depending on which list it is in.

            if (list == afterOthers) {
                //The list we're looking at is the afterOthers. If b is in the beforeOthers or noOthers then
                //by definition a is after b because a is in the afterOthers list.
                return true;
            } else if (list == beforeOthers) {
                //The list we're looking at is beforeOthers, and contains a and will be before
                //everything else in the final ist. If b is in the afterOthers list, then a cannot be before b.
                final WebFragmentResource bResource = getResourceForFragment(list, fragNameB);
                if (afterOthers.contains(bResource)) {
                    throw new IllegalStateException("Incorrect relationship: " + fragNameB + " after " + fragNameA);
                } else {
                    return false; //b could be moved from noOthers list
                }
            }
        }

        return true; //a and b in the same list, a is after b
    }

    /**
     * Insert the resource matching the fragName into the list of resources at the location indicated by index.
     * 
     * @param list
     * @param index
     * @param fragName
     */
    private void insert(final List<WebFragmentResource> list, final int index, final String fragName) {
        final WebFragmentResource jar = getResourceForFragment(list, fragName);
        if (jar == null) {
            throw new IllegalStateException("No jar for insertion");
        }

        insert(list, index, jar);
    }

    private void insert(final List<WebFragmentResource> list, final int index, final WebFragmentResource resource) {
        if (list == null) {
            throw new IllegalStateException("List is null for insertion");
        }

        //add it at the end
        if (index > list.size()) {
            list.add(resource);
        } else {
            list.add(index, resource);
        }
    }

    private int getIndexOf(final List<WebFragmentResource> resources, final String fragmentName) {
        final WebFragmentResource r = getResourceForFragment(resources, fragmentName);
        if (r == null) {
            return -1;
        }

        return resources.indexOf(r);
    }
}