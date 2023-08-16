TWGVideoClientGUI {
	var <client;
	var <win;

	var <connectedText, <showText, <modeTog, <gRWTog, <gFFTog, <gFFSpeedNum, <gPPTog, <gResetBut, <gClearRoutingBut,
	<routingGrid,
	<bMediaMenu, <bResetBut, <bRWTog, <bPPTog, <bFFTog, <bSpeedNum, <bPosNum, <bPosSlider, <bLevel, <bGainSlider, <bGainText,
	<presetNum, <presetRetriggerBut,
	<reconnectBut;

	var <>busButtonAction, <>busButtonShiftAction, <>routingButtonAction, <>routingButtonShiftAction;

	*new {|client|
		^super.newCopyArgs(client).init;
	}


	init {
		var title_font = Font("Input Sans", 24);
		var body_font = Font("Input Sans", 12);
		var small_font = Font.sansSerif(10).boldVariant;
		var smaller_font = Font.sansSerif(8).boldVariant;
		var bounds = Rect(0, 0, 460, 620);
		var m = 12;
		var headerView, globalView, routingView, busView;
		var ppImage, ffImage, rwImage;

		bMediaMenu = Array.newClear(5);
		bResetBut = Array.newClear(5);
		bRWTog = Array.newClear(5);
		bPPTog = Array.newClear(5);
		bFFTog = Array.newClear(5);
		bSpeedNum = Array.newClear(5);
		bPosNum = Array.newClear(5);
		bPosSlider = Array.newClear(5);
		bLevel = Array.newClear(5);
		bGainSlider = Array.newClear(5);
		bGainText = Array.newClear(5);

		ppImage = Image.open("icons/pp.png".resolveRelative);
		ffImage = Image.open("icons/ff.png".resolveRelative);
		rwImage = Image.open("icons/rw.png".resolveRelative);

		win = Window("TWG Video Client: " + client.name, bounds.center_(Point(1200, 500))).front;
		win.onClose = {client.quit};

		//win.bounds = win.bounds.resizeBy(0, -15);

		headerView = {|win, bounds|
			var view = View(win, bounds).background_(Color.gray(0.95));
			connectedText = StaticText(view, Rect(6, 3, bounds.width, 20)).string_("Not Connected.").font_(body_font);

			showText = StaticText(view, Rect(6, 25, bounds.width, 20)).string_("Show: ").font_(body_font);

			reconnectBut = Button(view, Rect(bounds.width-80, 3, 75, 20)).string_("Reconnect").font_(body_font).action_({
				client.connect;
			});
			modeTog = Button(view, Rect(bounds.width-80, 26, 75, 20)).states_([["CONTROL"], ["MONITOR"]]).font_(body_font).value_(if (client.mode==\control) {0} {1}).action_(
				{|x|
					var uiObjects = [gRWTog, gFFTog, gFFSpeedNum, gPPTog, gResetBut, gClearRoutingBut, presetNum, presetRetriggerBut] ++ bResetBut ++ bRWTog ++ bPPTog ++ bFFTog ++ bSpeedNum ++ bPosNum ++ bPosSlider ++ bGainSlider ++ bMediaMenu ++ Array.fill(30, {|i| routingGrid[(i%6)][(i/6).floor]});
					var newmode = switch (x.value, 0, {\control}, 1, {\monitor});
					uiObjects.do(_.enabled_(1-x.value));
					if (client.mode != newmode) {client.mode = newmode;}
			});
		};

		headerView.(win, Rect(m, m, bounds.width-(m*2)-170, 53));



		globalView = {|win, bounds|
			var view = View(win, bounds);

			gRWTog = Button(view, Rect(0, 0, 45, 40)).states_([[""], ["", Color.black, Color.white]]).font_(body_font).icon_(rwImage).iconSize_(25).action_({|x| bRWTog.do(_.valueAction_(x.value)); gPPTog.value_(0); gFFTog.value_(0)});
			gPPTog = Button(view, Rect(48, 0, bounds.width-96, 40)).states_([[""], ["", Color.black, Color.white]]).font_(body_font).icon_(ppImage).iconSize_(28).action_({|x| bPPTog.do(_.valueAction_(x.value)); gRWTog.value_(0); gFFTog.value_(0)});
			gFFTog = Button(view, Rect(bounds.width-46, 0, 45, 40)).states_([[""], ["", Color.black, Color.white]]).font_(body_font).icon_(ffImage).iconSize_(25).action_({|x| bFFTog.do(_.valueAction_(x.value)); gPPTog.value_(0); gRWTog.value_(0)});

			StaticText(view, Rect(0, 46, 60, 19)).string_("FF Speed:").font_(body_font);
			gFFSpeedNum = NumberBox(view, Rect(60, 46, 40, 19)).decimals_(2).scroll_step_(0.1).font_(body_font).value_(12).action_({|x| client.control.ffspeed_(x.value)});
			//Button(view, Rect(96, 30, 40, 20)).string_("FF").font_(body_font);
			StaticText(view, Rect(103, 46, 120, 19)).string_("Video Preset:").font_(body_font);
			presetNum = NumberBox(view, Rect(179, 46, 25, 19)).font_(body_font).action_({|x| client.control.preset_(x.value)});
			presetRetriggerBut = Button(view, Rect(209, 45, bounds.width-209, 19)).string_("Retrigger").font_(body_font).action_({
				client.control.preset_(force: true);
			});

			gResetBut = Button(view, Rect(0, 70, (bounds.width/2)-2, 23)).string_("Reset All").font_(body_font).action_({ |x|
				bResetBut.do(_.valueAction_(x.value));
				gRWTog.value_(0); gFFTog.value_(0); gRWTog.value_(0); gFFTog.value_(0); gPPTog.value_(0)
		});
			gClearRoutingBut = Button(view, Rect(bounds.width/2, 70, (bounds.width/2)-2, 23)).string_("Clear Routing").font_(body_font).action_(
				//{|x| bResetBut.do(_.valueAction_(x.value)); gRWTog.value_(0); gFFTog.value_(0); gRWTog.value_(0); gFFTog.value_(0); gPPTog.value_(0)}
				{client.control.routing_([])}
			);
		};

		globalView.(win, Rect(m, 70, bounds.width-(m*2)-170, 180));


		routingView = {|win, bounds|
			var view = View(win, bounds);
			/*			var routingAction = {
			var pairs = [];
			6.do({|r| 5.do({|c| if (routingGrid[r][c].value) {pairs = pairs.addAll([c, r])}})});
			pairs.postln;
			client.control.routing(*pairs);
			};*/
			routingGrid = Array.fill(6, { |row|
				Array.fill(5, { |col|
					CheckBox(view, Rect(60 + (col * 23), 15 + (row * 23), 20, 20))
					//.action_({ |checkbox| client.control.routing(row, col, if (checkbox.value) {1} {0})
					.action_({|x|
						var busKeys = [\a, \b, \c, \d, \e];
						var chanKeys = [\ear1, \ear2, \ear3, \room, \xtra, \phones];
						if (x.value)
						{client.control.route(busKeys[col], chanKeys[row])}
						{client.control.unroute(busKeys[col], chanKeys[row])}
					});
				})
			});


			Button(view, Rect(60 - 33, 0, 25, 15)).font_(small_font.copy.bold_(false)).string_("all").mouseDownAction_({ |view, x, y, modifiers| if (modifiers.isShift) { routingButtonShiftAction.value() } { routingButtonAction.value() } });
			5.do { |busno|
				Button(view, Rect(60 + (busno * 23), 0, 15, 15)).font_(small_font).string_((busno + 65).asAscii).mouseDownAction_({ |view, x, y, modifiers| if (modifiers.isShift) { routingButtonShiftAction.value(client.control.buses[busno]) } { routingButtonAction.value(client.control.buses[busno]) } });
			};
			["Ear 1", "Ear 2", "Ear 3", "Room", "Xtra", "(Phones)"].do { |name, i|
				StaticText(view, Rect(0, 17 + (i * 23), 52, 15)).font_(small_font).string_(name).align_(\right);
			};
		};

		routingView.(win, Rect(bounds.width-m-168, m, 168, 200));

		busView = {|win, bounds, index|
			var view = View(win, bounds).background_(Color.gray(0.8));

			Button(view, Rect(5, 5, 30, 20)).states_([[(index + 65).asAscii]]).font_(Font("Input Sans", 20, bold: true)).mouseDownAction_({ |view, x, y, modifiers| if (modifiers.isShift) { busButtonShiftAction.value(client.control.buses[index]) } { busButtonAction.value(client.control.buses[index]) } });

			StaticText(view, Rect(40, 6, 100, 20)).string_("Media:").font_(body_font);
			bMediaMenu[index] = PopUpMenu(view, Rect(80, 6, 245, 20)).items_(["", ""]).font_(body_font).action_({|x|
				client.control.buses[index].set(\media, x.value, \position, 0, \transport, \paused);
				bSpeedNum[index].value_(1);
				this.setPos(index, 0);
				this.setGain(index, 0);
			});
			bResetBut[index] = Button(view, Rect(330, 6, 42, 20)).string_("Reset").font_(body_font).action_({
				client.control.buses[index].reset;
				bMediaMenu[index].value_(0);
				bSpeedNum[index].value_(1);
				this.setPos(index, 0);
				this.setGain(index, 0);
			};);

			bRWTog[index] = Button(view, Rect(6, 30, 55, 20)).states_([[""], ["", Color.black, Color.white]]).font_(body_font).icon_(rwImage).iconSize_(11).action_(
				{|x| switch (x.value,
					0, {client.control.buses[index].pause; this.setTransport(index, \paused)},
					1, {client.control.buses[index].rw; this.setTransport(index, \rw)});
			});
			bPPTog[index] = Button(view, Rect(65, 30, 55, 20)).states_([[""], ["", Color.black, Color.white]]).font_(body_font).icon_(ppImage).iconSize_(12).action_(
				{|x| switch (x.value,
					0, {client.control.buses[index].pause; this.setTransport(index, \paused)},
					1, {client.control.buses[index].play; this.setTransport(index, \playing)});
			});
			bFFTog[index] = Button(view, Rect(126, 30, 55, 20)).states_([[""], ["", Color.black, Color.white]]).font_(body_font).icon_(ffImage).iconSize_(11).action_(
				{|x| switch (x.value,
					0, {client.control.buses[index].pause; this.setTransport(index, \paused)},
					1, {client.control.buses[index].ff; this.setTransport(index, \ff)});
			});
			StaticText(view, Rect(189, 30, 42, 20)).string_("Speed:").font_(body_font);
			bSpeedNum[index] = NumberBox(view, Rect(231, 30, 42, 20)).decimals_(2).scroll_step_(0.1).font_(body_font).value_(1).action_({|x| client.control.buses[index].speed_(x.value)});

			StaticText(view, Rect(278, 30, 60, 20)).string_("Position:").font_(body_font);
			bPosNum[index] = NumberBox(view, Rect(330, 30, 42, 20)).decimals_(2).scroll_step_(0.01).clipLo_(0).clipHi_(100).font_(body_font).action_({|x|
				client.control.buses[index].position_(x.value);
				//bPosSlider[index].value_(x.value/100);
			});
			bPosSlider[index] = Slider(view, Rect(6, 54, bounds.width-70, 15)).action_({|x| bPosNum[index].valueAction_(x.value*100)});

			bLevel[index] = 2.collect({|i| LevelIndicator(view, Rect((bounds.width-57)+(i*15), 6, 10, bounds.height-20))
				.warning_(0.9).critical_(1.0).drawsPeak_(true).numTicks_(9).numMajorTicks_(3);});

			bGainSlider[index] = Slider(view, Rect(bounds.width-23, 4, 18, bounds.height-24)).action_({|x|
				var dbval = if (x.value>0) {x.value.linlin(0, 1, -80, 0)} {-inf};
				client.control.buses[index].db_(dbval);
				bGainText[index].string = if (dbval == -inf) {"-inf"} {dbval.round(1).asInteger.asString};
			});
			bGainText[index] = StaticText(view, Rect(bounds.width-23, 34, 18, bounds.height-12)).string_("").font_(smaller_font).align_(\center);
		};

		5.do({|i| busView.(win, Rect(m, 173+(90*i), bounds.width-(m*2), 80), i)});



		win;

		//function to attempt to connect to server

	}

	setPos {|bus, val|
		bPosNum[bus].value = val;
		bPosSlider[bus].value = val/100;
	}

	setGain {|bus, val|
		bGainText[bus].string = if (val == -inf) {"-inf"} {val.round(1).asInteger.asString};
		bGainSlider[bus].value = val.linlin(-80, 0, 0, 1);
	}

	setTransport {|bus, val|
		case
		{val == \paused} {bPPTog[bus].value_(0); bFFTog[bus].value_(0); bRWTog[bus].value_(0)}
		{val == \playing} {bPPTog[bus].value_(1); bFFTog[bus].value_(0); bRWTog[bus].value_(0)}
		{val == \ff} {bPPTog[bus].value_(0); bFFTog[bus].value_(1); bRWTog[bus].value_(0)}
		{val == \rw} {bPPTog[bus].value_(0); bFFTog[bus].value_(0); bRWTog[bus].value_(1)};
	}
}