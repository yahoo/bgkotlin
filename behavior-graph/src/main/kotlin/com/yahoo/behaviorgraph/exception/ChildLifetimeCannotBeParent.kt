package com.yahoo.behaviorgraph.exception

import com.yahoo.behaviorgraph.ExtentLifetime

class ChildLifetimeCannotBeParent(child: ExtentLifetime): BehaviorGraphException("Child lifetime cannot be a transitive parent.")