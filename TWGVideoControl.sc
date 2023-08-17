TWGVideoControl {
  var <client;
  var <ffspeed, <preset, <routing, <a, <b, <c, <d, <e, <buses;
  var keyToInt, blankMatrix;

  *new {|client|
    ^super.newCopyArgs(client).init;
  }

  init {
    a = TWGVideoBusControl(this, 0);
    b = TWGVideoBusControl(this, 1);
    c = TWGVideoBusControl(this, 2);
    d = TWGVideoBusControl(this, 3);
    e = TWGVideoBusControl(this, 4);
    buses = [a, b, c, d, e];
    ffspeed = 12;
    routing = (\a: [], \b: [], \c: [], \d: [], \e: []);
  }

  set { |...pairs|
    var msg = Array.fill(40, "n");
    var msgString = "";
    var setRouting = routing;
    var routingChanged = false;

    //pairs.debug("TWGVideoControl set");

    var process = { |argarray|

      argarray.pairsDo { |key, val|
        var busids = (\a: 0, \b: 1, \c: 2, \d: 3, \e: 4);

        case
        {busids.includesKey(key) && val.isArray} {
          var busindex = busids[key], bus = buses[busindex];
          val.pairsDo({|k, v|
            case
            {k == \media} {
              msg[(busindex*7)] = v;
              buses[busindex].media_(v, hard: false);
            }
            {k == \position} {
              msg[(busindex*7)+1] = v;
              buses[busindex].position_(v, hard: false);
            }
            {k == \db } {
              msg[(busindex*7)+5] = v;
              buses[busindex].db_(v, hard: false);
            }
            {k == \speed} {
              var speed, ramp = 0, curve = 3, pitch = 0;
              case
              {v.isArray} {
                speed = v[0];
                ramp = v[1] ? 0;
                curve = v[2] ? 3;
                pitch = v[3].asBoolean.asInteger ? 0
              }
              {v.isNumber} {speed = v};

              if (curve.class == Symbol) {curve = Env.shapeNames[curve]};

              msg[(busindex*7)+2] = speed.asString + ramp.asString + curve.asString + pitch.asString;
              buses[busindex].speed_(speed, hard: false);
            }
            {k == \loop} {
              var on, start, end;
              case
              {v.isArray} {
                on = v[0].asBoolean.asInteger ?? buses[busindex].loop;
                start = v[1] ?? buses[busindex].loopstart;
                end = v[2] ?? buses[busindex].loopend;
              } {
                on = v.asBoolean.asInteger;
                start = buses[busindex].loopstart;
                end = buses[busindex].loopend;
              };
              msg[(busindex*7)+6] = (on.asString + start.asString + end.asString);
              buses[busindex].loop_(on.asBoolean, start, end, hard: false);
            }
            {k == \transport} {
              msg[(busindex*7)+4] = (\playing: 1, \paused: 0, \ff: 2, \rw: -1)[v] ?? "n";
              buses[busindex].transport_(v, hard: false);
            }
            {k == \routing} {
              routingChanged = true;
              v = if (v.isArray) {v} {[v]};
              setRouting[key] = v;
            }
          })
        }
        {[\media, \position, \speed, \db, \loop, \transport].includes(key)} {
          busids.pairsDo({|k, v| process.([k, [key, val]])})
        }

        {key == \preset} {
          msg[24] = val; //bus D zoom
          this.preset_(val, hard: false);
        }
        {(key == \ffspeed) && val.notNil} {
          msg[17] = val; //bus C zoom
          this.ffspeed_(val, hard: false);
        }
        {key == \forcepreset && val} {msg[31] = 1}
        {(key == \routing) && val.isArray} {
          routingChanged = true;
          setRouting = (\a: [], \b: [], \c: [], \d: [], \e: []);
          val.pairsDo {|bus, chan|
            bus = if (bus.isArray) {bus} {[bus]};
            chan = if (chan.isArray) {chan} {[chan]};
            bus.do({ |bus|
              setRouting[bus] = setRouting[bus].addAll(chan);
            })
          }
        }
      }
    };

    process.(pairs);

    if (routingChanged) {
      var matrix = 90.collect({|i|
        switch (i%3,
          0, {(i/3).asInteger%5},
          1, {(((i-1)/3)/5).floor.asInteger},
          2, {0}
        );
      });
      var matrixString = "";
      var busKeys = [\a, \b, \c, \d, \e];
      var chanKeys = [\ear1, \ear2, \ear3, \room, \xtra, \phones];
      busKeys.do({|bus|
        setRouting[bus].do({|chan|
          var bnum = busKeys.indexOf(bus);
          var cnum = chanKeys.indexOf(chan);
          if (cnum.notNil) {matrix[(cnum*15)+2+(3*bnum)] = 1};
        })
      });
      matrix.size.do({ |i| matrixString = matrixString + matrix[i].asString});
      msg = msg.add(matrixString[1..]);

      this.routing_(*setRouting.asPairs ++ false);
    };

    msg.debug("set");
    if (client.mode == \control) {client.serverAddress.sendMsg('/fromsm', *msg)};

  }

  play { |...pairs|
    this.set(\transport, \playing, *pairs);
  }

  pause { |...pairs|
    this.set(\transport, \paused, *pairs);
  }

  ff { |setFFspeed ...pairs|
    this.ffspeed_(setFFspeed);
    this.set(\transport, \ff, *pairs);
  }

  rw { |setFFspeed ...pairs|
    this.set(\transport, \rw, \ffspeed, setFFspeed, *pairs);
  }

  cue { |media, position, speed, db, loop|
    this.play(
      \media, media ? 1,
      \position, position ? 0,
      \speed, speed ? 1,
      \db, db ? 0,
      \loop, loop ? false
    )
  }

  reset { |...pairs|
    this.pause(\media, 0, \position, 0, \speed, 1, \db, 0, \loop, [false, 0, 100], \preset, 1, \routing, [], \ffspeed, 8, *pairs);
    //this.set(\preset, 1, \routing, [], \ffspeed, 8, *pairs);
  }

  // set values for all buses

  media_ { |val|
    this.set(\media, val);
  }

  position_ { |val|
    this.set(\position, val);
  }

  speed_ { |...vals|
    this.set(\speed, vals);
  }

  db_ { |val|
    this.set(\db, val);
  }

  loop_ { |...vals|
    this.set(\loop, vals);
  }

  transport_{ |val|
    this.set(\transport, val);
  }

  // set global values

  ffspeed_ { |val, hard = true|
    if (ffspeed != val && ffspeed.notNil) {
      if (hard) {this.set(\ffspeed, val)};
      ffspeed = val;
      {client.gui.gFFSpeedNum.value_(ffspeed)}.defer;
    }
  }

  routing_ { |...vals|
    var hard = true;
    var setGUI = Array2D.fromArray(5, 6, 30.collect({0}));

    if (vals.size==1 && vals[0].isArray) {vals = vals[0]};
    if (vals.size.odd) {hard = vals.pop};

    if (hard) {this.set(\routing, vals)};

    routing = (\a: [], \b: [], \c: [], \d: [], \e: []);

    vals.pairsDo({ |bus, chan|
      bus = if (bus.isArray) {bus} {[bus]};
      chan = if (chan.isArray) {chan} {[chan]};
      bus.do({ |bu|
        chan.do({ |ch|
          var bnum, cnum;
          routing[bu] = routing[bu].add(ch).asSet.asArray;
          bnum = [\a, \b, \c, \d, \e].indexOf(bu);
          cnum = [\ear1, \ear2, \ear3, \room, \xtra, \phones].indexOf(ch);
          if (bnum.notNil && cnum.notNil) {setGUI[bnum, cnum] = 1};
        })
      });
    });
    5.do({|bu| 6.do{|ch| {client.gui.routingGrid[ch][bu].value_(setGUI[bu, ch])}.defer}});
  }

  route { |...vals|
    var hard = true;
    var setRouting = routing;
    if (vals.size.odd) {hard = vals.pop};


    vals.pairsDo({ |bus, chan|
      bus = if (bus.isArray) {bus} {[bus]};
      chan = if (chan.isArray) {chan} {[chan]};
      bus.do({ |bu|
        setRouting[bu] = setRouting[bu].addAll(chan).asSet.asArray;
      })
    });

    setRouting = setRouting.asPairs;
    this.set(\routing, setRouting ++ hard);
  }

  unroute { |...vals|
    var hard = true;
    var setRouting = routing;
    if (vals.size.odd) {hard = vals.pop};

    vals.pairsDo({ |bus, chan|
      bus = if (bus.isArray) {bus} {[bus]};
      chan = if (chan.isArray) {chan} {[chan]};
      bus.do({ |bu|
        setRouting[bu] = setRouting[bu].removeAll(chan);
      })
    });
    setRouting = setRouting.asPairs;
    this.set(\routing, setRouting ++ hard);
  }

  preset_ { |val, force = false, hard = true|

    if (preset != val && val.isNumber) {
      if (hard) {this.set(\preset, val, \forcepreset, force)};
      preset = val;
      {client.gui.presetNum.value_(preset)}.defer;
    } {
      if (force) {this.set(\forcepreset, true)};
    }
  }

  forcepreset { |val|
    this.preset_(val, force: true)
  }

}



//
// {key == \media} {[0, 7, 14, 21, 28].do({|i| msg[i] = val})}
// {key == \position} {[1, 8, 15, 22, 29].do({|i| msg[i] = val})}
// {key == \speed} {
//   var speed, ramp = 0, curve = 1, pitch = 0;
//   case
//   {val.isArray} {
//     speed = val[0];
//     ramp = val[1] ?? 0;
//     curve = val[2] ?? 1;
//     pitch = val[3].asBoolean.asInteger ?? 0;
//   }
//   {val.isNumber} {speed = val};
//
//   buses.do({|bus, i|
//     msg[(i*7)+2] = speed.asString + ramp.asString + curve.asString + pitch.asString;
//     buses[i].speed_(speed, hard: false);
//   })
// }
// {key == \db} {
//   buses.do({|bus, i|
//     msg[(i*7)+5] = val;
//     buses[i].db_(val, hard: false);
//   })
// }
// {key == \transport} {
//   buses.do({|bus, i|
//     msg[(i*7)+4] = (\playing: 1, \paused: 0, \ff: 2, \rw: -1)[val] ?? "n";
//     buses[i].transport_(val, hard: false);
//   })
// }