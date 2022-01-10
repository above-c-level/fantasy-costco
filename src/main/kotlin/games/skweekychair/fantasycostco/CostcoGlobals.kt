package games.skweekychair.fantasycostco

object CostcoGlobals {
    // Mass to add to each transaction
    val massPerTransaction = 1.0

    // This limits how high the mass of a commodity can go. This means that
    // selling/buying can always modify price a little bit
    val maximumMass = 1000.0

    // This prevents the shown price from changing *too* quickly
    val maxPctChange = 0.05

    // This affects how much variation the random noise has based on price
    // 1/50 seems to be a solid value
    val varMultiplier = 1.0 / 50.0

    // This affects the variation based on mass, so that when `mass` approaches 0,
    // variation is multiplied by `massVarMin`, and when `mass` approaches
    // `maximumMass`, the variation is multiplied by `massVarMax`
    val massVarMin = 1.0
    val massVarMax = 1.0 / 10.0

    // 0.05 is a spread of 5% at the most ideal price
    val priceSpread = 0.05

    // 0.001 results in a price difference between 1 item and a stack of 64 of:
    // 9.37 and 10 for sell prices (10 is the idealized price in this example)
    // 10.63 and 10 for buy prices (10 is the idealized price in this example)
    val surchargeCurveEpsilon = 0.001

    val buyMult = (1.0 + priceSpread / 2.0)

    val sellMult = (1.0 - priceSpread / 2.0)
}
