import numpy as np
from plotly.graph_objects import Scatter, Figure

# The idea here is that the more transactions we have, the more intertia
# The higher the inertia, the harder it is to move the price
mass = 5
mass_per_transaction = 1
# This limits how high the mass of a commodity can go. This means that
# selling/buying can always modify price a little bit
maximum_mass = 1000
# This prevents the shown price from changing *too* quickly
max_pct_change = 0.05
# The starting and hidden price of the commodity
hidden_price = 500
# This affects how much variation the random noise has based on price
# 1/50 seems to be a solid value
var_multiplier = 1 / 50
# This affects the variation based on mass, so that when `mass` approaches 0,
# variation is multiplied by `mass_var_min`, and when `mass` approaches
# `maximum_mass`, the variation is multiplied by `mass_var_max`
mass_var_min = 1
mass_var_max = 1 / 10
# 0.05 is a spread of 5% at the most ideal price
price_spread = 0.05
# 0.001 results in a price difference between 1 item and a stack of 64 of:
# 9.37 and 10 for sell prices (10 is the idealized price)
# 10.63 and 10 for buy prices (10 is the idealized price)
surcharge_curve_epsilon = 0.001
# Start out with the shown price the same
shown_price = hidden_price
buy_mult = (1 + price_spread / 2)
sell_mult = (1 - price_spread / 2)


def buy_price(input_price, amount, stack_size=64):
    """
    Returns the buy price given given an ideal price, the number of items
    being purchased, and the maximum amount that can be held in one stack
    """
    hyperbola = surcharge_curve_epsilon * stack_size / amount
    price_offset = surcharge_curve_epsilon * input_price
    return buy_mult * input_price * (1 + hyperbola) - price_offset


def sell_price(input_price, amount, stack_size=64):
    """
    Returns the sell price given given an ideal price, the number of items
    being purchased, and the maximum amount that can be held in one stack
    """
    hyperbola = -surcharge_curve_epsilon * stack_size / amount
    price_offset = surcharge_curve_epsilon * input_price
    return buy_mult * input_price * (1 + hyperbola) + price_offset


def lerp(x, x_0, y_0, x_1, y_1):
    """
    Linearly interpolates `x` between the points (x_0, y_0) and (x_1, y_1)
    """
    # Literally just the formula for linear interpolation
    # if you have a function `func` that goes between 0 and 1, you can also
    # interpolate with that function by replacing it with
    # (y_1 - y_0) * func((x - x_0) / (x_1 - x_0)) + y_0
    # For more, see here: https://www.desmos.com/calculator/6wh1xdmhc5
    return (y_1 - y_0) * ((x - x_0) / (x_1 - x_0)) + y_0


def smoothstep(x):
    """https://en.wikipedia.org/wiki/Smoothstep"""
    if x <= 0:
        return 0
    elif x >= 1:
        return 1
    else:
        return 3 * x**2 - 2 * x**3


def lerp_clamp(x, upper_bound, lower_bound, multiplier=0.25):
    """
    Combination of lerp and clamp. Linearly interpolates between 0 and
    some `multiplier` between `lower_bound` and `upper_bound`, but it
    also clamps the values between 0 and `multiplier`.
    https://www.desmos.com/calculator/dzqgq1bkyw
    """
    if x <= lower_bound:
        return 0
    elif x >= upper_bound:
        return multiplier
    diff = upper_bound - lower_bound
    return multiplier * x / diff - (lower_bound * multiplier) / diff


def smooth_price():
    """
    Prevents the shown price from moving more than `max_pct_change` than its
    previous value. For example, if the hidden price moves from 1 to 2 but
    `max_pct_change` is 0.1, the new shown price is 1.10.
    """
    global shown_price
    upper_limit = (1 + max_pct_change) * shown_price
    lower_limit = (1 - max_pct_change) * shown_price
    if hidden_price < upper_limit and hidden_price > lower_limit:
        shown_price = hidden_price
    elif hidden_price > upper_limit:
        shown_price = upper_limit
    else:
        shown_price = lower_limit


def push_amount():
    """
    Calculates the magnitude of the change of price given a transaction
    """
    dist = abs(shown_price - hidden_price) + 1
    sqrt_price = np.sqrt(shown_price)
    return np.sqrt(1 / mass * dist * sqrt_price)


def add_mass():
    """
    Increases the 'mass' of the commodity,
    making it harder to move in the future
    """
    global mass
    if mass < mass_per_transaction:
        mass += mass_per_transaction
        mass = min(maximum_mass, mass)


# position
x_vals = []
y_vals = []
y2 = []
for i in range(100):
    x_vals.append(i)
    y_vals.append(shown_price)
    y2.append(hidden_price)
    ############################################################################
    # ! IN THE ACTUAL PLUGIN, WE'LL ALLOW FOR TRADING UP TO A FULL STACK AT ONCE
    # ! THIS PYTHON TEST FILE IS DESIGNED FOR BUYING/SELLING ONLY 1 ITEM AT ONCE
    ############################################################################
    buy = buy_price(shown_price, 1, stack_size=64)
    sell = sell_price(shown_price, 1, stack_size=64)
    while True:
        end = False
        response = input(
            f"Current buy/sell price is {buy:.2f}/{sell:.2f}. [b]uy, [s]ell, [h]old, or [q]uit?"
        )
        response = response.lower()
        if len(response) == 0:
            response = 'h'
        if response == "h":
            smooth_price()

            break
        elif response == "b":
            hidden_price += push_amount()
            smooth_price()
            add_mass()
            break
        elif response == "s":
            hidden_price -= push_amount()
            smooth_price()
            add_mass()
            break
        elif response == "q":
            end = True
            break
        else:
            print("Invalid response")

    if end:
        break
    # hopefully this is either unnecessary or doesn't happen often
    # but just in case
    hidden_price = abs(hidden_price)
    mass_var = lerp(mass, 0, mass_var_min, maximum_mass, mass_var_max)
    variation = hidden_price * var_multiplier * mass_var
    scale = variation
    hidden_price += np.random.normal(loc=hidden_price,
                                     scale=scale) - hidden_price
    dist = abs(hidden_price - shown_price)
    correction_gain = lerp_clamp(dist, hidden_price, hidden_price / 2, 0.5)
    shown_price = abs((1 - correction_gain) * shown_price +
                      correction_gain * hidden_price)

data = []
trace = Scatter(
    x=x_vals,
    y=y_vals,
    mode='lines',
    connectgaps=False,
)
data.append(trace)
# trace = Scatter(
#     x=x_vals,
#     y=y2,
#     mode='lines',
#     connectgaps=False,
# )
# data.append(trace)
fig = Figure(data=data)
fig.show()
