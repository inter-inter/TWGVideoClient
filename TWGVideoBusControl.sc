TWGVideoBusControl {
	var <parent, <index, busSym;
	var <media, <position, <speed, <db, <transport, <loop, <loopstart, <loopend, routing;

	*new {|parent, index|
		^super.newCopyArgs(parent, index).init;
	}

	init {
		transport = \paused;
		busSym = switch (index,
			0, {\a},
			1, {\b},
			2, {\c},
			3, {\d},
			4, {\e}
		)
	}

	set {|...pairs|
		parent.set(busSym, pairs)
	}

	play {|...pairs|
		parent.set(busSym, [\transport, \playing] ++ pairs)
	}

	pause {|...pairs|
		parent.set(busSym, [\transport, \paused] ++ pairs)
	}

	ff { |val ...pairs|
		parent.set(\ffspeed, val, busSym, [\transport, \ff] ++ pairs)
	}

	rw { |val ...pairs|
		parent.set(\ffspeed, val, busSym, [\transport, \rw] ++ pairs)
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
		this.pause(\media, 0, \position, 0, \speed, 1, \db, 0, \loop, [false, 0, 100], *pairs);
	}

	media_ { |val, hard = true|
		//if (media != val) {
			if (hard) {this.set(\media, val)};
			media = val;
			{
        var gui = parent.client.gui;
        var menuIndex = (gui.mediaNums.indexOf(val.asInteger) ? -1) + 1;
        gui.bMediaMenu[index].value_(menuIndex)}.defer;
		//}
	}

	position_ { |val, hard = true|
		//if (position != val) {
			if (hard) {this.set(\position, val)};
			position = val;
			{parent.client.gui.setPos(index, val)}.defer;
		//}
	}

	speed_ { |val, ramp = 0, curve = 3, pitch = 0, hard = true|
		if (val.isArray) {
      ramp = val[1] ? 0;
      curve = val[2] ? 3;
      pitch = val[3].asBoolean.asInteger ? 0;
			val = val[0] ? speed;
		};
		//if (speed != setSpeed) { // removing this because it would be impossible to change curve or pitch while keeping speed the same
    speed = val ? speed;
    if (hard) {this.set(\speed, [speed, ramp, curve, pitch])};
    {parent.client.gui.bSpeedNum[index].value_(speed)}.defer;
		//}
	}

	db_ { |val, hard = true|
		//if (db != val) {
			if (hard) {this.set(\db, val)};
			db = val;
			{parent.client.gui.setGain(index, db)}.defer;
		//}
	}

	transport_ { |val, hard = true|
		//if (transport != val) {
			if (hard) {this.set(\transport, val)};
			transport = val;
			{parent.client.gui.setTransport(index, transport)}.defer;
		//}
	}

  loop_ { |on, start, end, hard = true|
		if (on.isArray) {
      # on, start, end = on
		};

    //if (((loop == on) && (loopstart == start) && (loopend == end)).not ? true) {
      loop = on ? loop ? 0;
      loopstart = start ? loopstart ? 0;
      loopend = end ? loopend ? 100;
      if (hard) {this.set(\loop, [loop, loopstart, loopend])};
      {
        parent.client.gui.bLoopTog[index].value_(loop);
        parent.client.gui.bLoopSlider[index].lo_(loopstart * 0.01).hi_(loopend * 0.01).knobColor_([Color.grey, Color.yellow][loop.asInteger]);
      }.defer;
		//}
	}

  loopstart_ { |val, hard = true|
    //if (loopstart != val) {
      loopstart = val ? loopstart;
			if (hard) {this.set(\loop, [loop, loopstart, loopend])};
			{parent.client.gui.bLoopSlider[index].lo_(loopstart * 0.01)}.defer;
		//}
  }

  loopend_ { |val, hard = true|
    //if (loopend != val) {
      loopend = val ? loopend;
			if (hard) {this.set(\loop, [loop, loopstart, loopend])};
			{parent.client.gui.bLoopSlider[index].hi_(loopend * 0.01)}.defer;
		//}
  }

	routing {
		^parent.routing[busSym];
	}

	routing_ { |...vals|
		this.parent.set(busSym, [\routing, vals]);
	}

	route { |...vals|
		this.parent.route(busSym, vals)
	}

	unroute { |...vals|
		this.parent.unroute(busSym, vals)
	}
}