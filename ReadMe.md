## Udacity P3 project Stock Hawk ##

### See assignment details at ###
   https://classroom.udacity.com/nanodegrees/nd801/parts/8011345406/modules/430205859175461/lessons/4302058591239847/concepts/42882736840923

### Description of work ###
The screenshots were taken earlier.
1. Improved the UI with material design features, such as color choice, CollapsingToolbarLayout, etc. See screenshots
1. Added widget. **Glitch**: initially designed with ListView and RemoteViewsService. But the RemoteViewsService did not update the widget (seems like a bug in RemoteViewsService). Changed to use basic layout widgets with IntentService.
1. Fixed the crashing bug and added empty list page. Removed the feature of automatically adding initial symbols when the list is empty.
1. Added RtoL capability and accessibility.
1. Added a detail panel that slides in when an item is clicked. The panel contains additional information about the symbol. **Glitch**: initially used Gridlayout, but later discovered that it did not display on some devices. Changed to use basic layout widgets.      

### Screenshots ###
![withDetail](https://github.com/mingrutar/stockHawk/blob/master/screenShots/prtraint-detail.png?raw=true)
![dialog](https://github.com/mingrutar/stockHawk/blob/master/screenShots/dialog.png?raw=true)
![Portrait](https://github.com/mingrutar/stockHawk/blob/master/screenShots/prtraint.png?raw=true)
![RtoL](https://github.com/mingrutar/stockHawk/blob/master/screenShots/RtoL.png?raw=true)
![widget](https://github.com/mingrutar/stockHawk/blob/master/screenShots/widgets.png?raw=true)
![landscape](https://github.com/mingrutar/stockHawk/blob/master/screenShots/land.png?raw=true)  
