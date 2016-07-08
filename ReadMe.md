## Udacity P3 project - Stock Hawk ##
![logo](https://github.com/mingrutar/stockHawk/blob/master/screenShots/stockHawk_logo_4.png?raw=true)

### See assignment details at ###
   https://classroom.udacity.com/nanodegrees/nd801/parts/8011345406/modules/430205859175461/lessons/4302058591239847/concepts/42882736840923

The screenshots of the original work

![portrait](https://github.com/mingrutar/stockHawk/blob/master/screenShots/v0-portrait.png?raw=true)
![landscape](https://github.com/mingrutar/stockHawk/blob/master/screenShots/v0-landscape.png?raw=true)

### Description of work ###

Added a detail fragment that contains stock history in a graphic plot. The user can select the time length of acquiring data via the panel at top of detail fragment.
The fab button turns to a navigation icon for collapsing the detail fragment. When the detail fragment is not present, the button has 'add' icon.

* added a fragment for stock plot of selected symbol.  * Improved the UI with material design features, such as color choice, CollapsingToolbarLayout, etc. See screenshots
* Added widget. *Glitch*: initially designed with ListView and RemoteViewsService. But the RemoteViewsService did not update the widget (seems like a bug in RemoteViewsService). Changed to use basic layout widgets with IntentService.
* Fixed the crashing bug and added empty list page. Removed the feature of automatically adding initial symbols when the list is empty.
* Added RtoL capability and accessibility.

### Screenshots ###
![Portrait](https://github.com/mingrutar/stockHawk/blob/master/screenShots/v2-portrait.png?raw=true)
![portraitDetail](https://github.com/mingrutar/stockHawk/blob/master/screenShots/v2-portrait-detail.png?raw=true)
![dialog](https://github.com/mingrutar/stockHawk/blob/master/screenShots/dialog.png?raw=true)
![landDetailText](https://github.com/mingrutar/stockHawk/blob/master/screenShots/v2-landscape-detail2.png?raw=true)
![widget](https://github.com/mingrutar/stockHawk/blob/master/screenShots/widgets.png?raw=true)
![landDetail](https://github.com/mingrutar/stockHawk/blob/master/screenShots/v2-landscape-detail.png?raw=true)
![landscape](https://github.com/mingrutar/stockHawk/blob/master/screenShots/v2-landscape.png?raw=true)
![RtoL](https://github.com/mingrutar/stockHawk/blob/master/screenShots/RtoL.png?raw=true)
