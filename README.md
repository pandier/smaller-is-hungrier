![icon](/assets/icon-64px.png)

# smaller-is-hungrier

**Smaller is Hungrier** is a [Sponge](https://spongepowered.org) plugin that makes player smaller when they are hungrier.
You can also make them smaller when they lose health ;)

Why? Because why not.

This is definitely an original idea that didn't come from [OwengeJuiceTV](https://twitch.tv/owengejuicetv)

## 📥 Installation

1. Download the `smaller-is-hungrier-x.x.x-all.jar` (`x.x.x` is the version) from the [latest release](https://github.com/pandier/smaller-is-hungrier/releases/latest)
2. Put the file into `mods` directory under your Sponge server
3. Run the server

## ⚙️ Configuration

```hocon
# Defines the range in which the resulted scale will be.
# 
# The high property defines the scale when the player has 20 hunger/health
# and the low property defines the scale when the player reaches 0 hunger/health.
# 
# You can also set the low value higher than the high value to make the player bigger when hungrier!
range {
  high=1.0
  low=0.45
}

# Defines the source which will be used to calculate the scale.
# 
# Possible values:
#  - food = uses the players food level with high at 20
#  - health = uses the players health with high at 20 (players will grow on excess health)
source=food
```
