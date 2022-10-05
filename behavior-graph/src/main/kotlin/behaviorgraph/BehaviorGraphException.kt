//
// Copyright Yahoo 2021
//
package behaviorgraph

open class BehaviorGraphException : RuntimeException {
    constructor(message: String, ex: Exception?): super(message, ex)
    constructor(message: String): super(message)
    constructor(ex: Exception): super(ex)
}
