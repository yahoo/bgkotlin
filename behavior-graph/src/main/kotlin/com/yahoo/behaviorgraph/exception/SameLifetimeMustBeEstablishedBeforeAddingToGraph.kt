package com.yahoo.behaviorgraph.exception

import com.yahoo.behaviorgraph.Extent

class SameLifetimeMustBeEstablishedBeforeAddingToGraph(ext: Extent): BehaviorGraphException("Same lifetime relationship must be established before adding any extent to graph.")