# Mouse Macro Recorder

The Mouse Macro Recorder plugin captures mouse movements and menu entry clicks in the client so you can reuse the
sequence later (e.g., for scripting or diagnostics).

## Plugin Configuration
Use the plugin configuration toggles to control recording:
- **Recording enabled**: clears existing events and begins capturing moves/clicks.
- **Clear recording**: removes all recorded events.
- **Export JSON to clipboard**: formats the current recording as JSON and copies it to the clipboard.

## Recorded format
Each event includes a timestamp offset, canvas coordinates, and (for clicks) the clicked `MenuEntry` metadata.

```json
{
  "startedAtEpochMs": 1716220000000,
  "events": [
    {
      "type": "MOVE",
      "offsetMs": 120,
      "x": 512,
      "y": 338,
      "menuEntry": null
    },
    {
      "type": "CLICK",
      "offsetMs": 420,
      "x": 516,
      "y": 340,
      "menuEntry": {
        "option": "Chop down",
        "target": "Tree",
        "type": "GAME_OBJECT_FIRST_OPTION",
        "identifier": 1276,
        "param0": 48,
        "param1": 57,
        "itemId": -1,
        "itemOp": 0,
        "worldViewId": 0,
        "forceLeftClick": false,
        "deprioritized": false,
        "widgetId": null
      }
    }
  ]
}
```

## Configuration
Movement sampling controls live under **Recording** in the plugin config:
- **Record mouse movement**: enable or disable movement tracking.
- **Movement sample interval (ms)**: minimum time between recorded move events.
- **Movement min distance**: minimum pixel distance for a movement sample to be kept.
- **Max recorded events**: stops recording after reaching the limit.
