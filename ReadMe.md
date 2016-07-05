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
* With detail panel
![alt](https://github.com/mingrutar/stockHawk/blob/master/screenShots/portait-detail.png?raw=true)
* The top image will slide up when scroll
![Portrait](https://github.com/mingrutar/stockHawk/blob/master/screenShots/portrait.png?raw=true)
* The widget
![widget](https://github.com/mingrutar/stockHawk/blob/master/screenShots/widget.png?raw=true)
* Dialog for adding symbols
![dialog](https://github.com/mingrutar/stockHawk/blob/master/screenShots/add-dialog.png?raw=true)
* Right to left locale
![RtoL](https://github.com/mingrutar/stockHawk/blob/master/screenShots/add-dialog.png?raw=true)
* Landscpe
![landscape](https://github.com/mingrutar/stockHawk/blob/master/screenShots/landscape.png?raw=true)  
