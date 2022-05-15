package com.yahoo.behaviorgraph.exception

import com.yahoo.behaviorgraph.Extent
import com.yahoo.behaviorgraph.ExtentLifetime

class ChildLifetimeCannotBeParent(child: Extent<*>): BehaviorGraphException("Child lifetime cannot be a transitive parent.")