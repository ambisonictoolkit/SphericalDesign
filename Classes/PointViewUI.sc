// A container view for the rotation/motion
// controls for manipulating PointView.

PointViewUI : View {
	var pv; // PointView

	var autoChks, invChks;
	var rotSls;
	var rotRadNbs, rotDegNbs, rotPerNbs;
	// show/hide: axes, indices, connections
	var axChk, idxChk, conChk;
	var headerTxts;
	var mstrLayout;

	// motion flags
	var rttModeChk, yprModeChk, radianChk, degreeChk;

	*new { |pointView, bounds = (Rect(0,0, 700, 250))|
		^super.new(pointView, bounds).init(pointView);
	}

	init { |pointView|

		pv = pointView;
		pv.addDependant(this);
		this.onClose_({ pv.removeDependant(this) });

		mstrLayout = VLayout().spacing_(2);
		this.layout_(mstrLayout);
		this.resize_(5);
		this.background_(Color.green.alpha_(0.25));

		// init controls
		autoChks = [\autoRotate, \autoTilt, \autoTumble].collect{ |method|
			CheckBox()
			.action_({ |ui|
				pv.perform((method ++ \_).asSymbol, ui.value)
			})
			.value_(pv.perform(method))
			;
		};

		// invert rotation directions of auto-rotate
		invChks = [\rotateDir, \tiltDir, \tumbleDir].collect{ |method|
			CheckBox()
			.action_({ |ui|
				pv.perform((method ++ \_).asSymbol, ui.value.asBoolean.if({-1},{1}))
			})
			.value_(pv.perform(method).asBoolean.not)
			;
		};

		// rotate sliders
		rotSls = [\rotate, \tilt, \tumble].collect{ |method|
			Slider()
			.action_({ |ui|
				pv.perform((method ++ \_).asSymbol, ui.value.linlin(0, 1, pi, -pi))
			})
			.value_(pv.perform(method).linlin(pi, -pi, 0, 1))
			.orientation_(\horizontal)
			.maxHeight_(45)
			;
		};

		// radian rotation NumberBoxes
		rotRadNbs = [\rotate, \tilt, \tumble].collect{ |method|
			NumberBox()
			.action_({ |ui|
				pv.perform((method ++ \_).asSymbol, ui.value * pi)
			})
			.clipLo_(-2).clipHi_(2)
			.step_(0.02).scroll_step_(0.02)
			.decimals_(2)
			.maxWidth_("-2.00".bounds.width * 1.3)
			.value_(pv.perform(method) / pi)
			.align_(\center)
			;
		};

		// degree rotation NumberBoxes
		rotDegNbs = [\rotate, \tilt, \tumble].collect{ |method|
			NumberBox()
			.action_({ |ui|
				pv.perform((method ++ \_).asSymbol, ui.value.degrad)
			})
			.clipLo_(-360).clipHi_(360)
			.step_(0.5).scroll_step_(0.5)
			.decimals_(1)
			.maxWidth_("-180.0".bounds.width * 1.3)
			.value_(pv.perform(method).degrad)
			.align_(\center)
			;
		};

		// rotation period NumberBoxes
		rotPerNbs = [\rotatePeriod, \tiltPeriod, \tumblePeriod].collect{ |method|
			NumberBox()
			.action_({ |ui|
				pv.perform((method ++ \_).asSymbol, ui.value)
			})
			.clipLo_(0.01).clipHi_(1000)
			.step_(0.5).scroll_step_(0.5)
			.decimals_(1)
			.maxWidth_("500.5".bounds.width * 1.3)
			.value_(pv.perform(method))
			.align_(\center)
			;
		};

		// labels above control columns
		headerTxts = ["Rotate/Tilt/Tumble", "pi", "deg", "auto", "T", "inv"].collect{|txt|
			StaticText()
			.string_(txt)
			.align_(\center)
			;
		};

		// motion flags
		// var rttModeChk, yprModeChk, radianChk, degreeChk;

		// rotSls.do(mstrLayout.add(_));
		// rotRadNbs.do(mstrLayout.add(_));
		// rotDegNbs.do(mstrLayout.add(_));
		// autoChks.do(mstrLayout.add(_));
		// rotPerNbs.do(mstrLayout.add(_));
		// invChks.do(mstrLayout.add(_));

		// [rotSls, rotRadNbs, rotDegNbs, autoChks, rotPerNbs, invChks].do({ |ctls, i|
		// 	var col;
		// 	col = ctls.insert(0, headerTxts[i]).add(nil);
		// 	mstrLayout.add(VLayout(*col))
		// });

		[
			PointViewMotionCtl( pv, \rotate, \rtt, \radians, pv.axisColors[2]),
			PointViewMotionCtl( pv, \tilt,   \rtt, \radians, pv.axisColors[0]),
			PointViewMotionCtl( pv, \tumble, \rtt, \radians, pv.axisColors[1]),
			nil
		].do(mstrLayout.add(_));
	}
}


// PointViewMotionCtl manages the controls for various rotation
// parameters used by PointView, one for each axis of rotation.
// State changes are broadcast from the UI and captured in
// the update method of the PointView, which is its dependent.
PointViewMotionCtl : View {

	var pv, mode, whichRot, units, specRad, specDeg, slSpec;

	// ui elements
	// primary rotation
	var sl, slLabel, slLabelL, slLabelR, rotRadNb, rotDegNb, rotLabel;
	// cyclic motion
	var cycPerNb;
	// oscillatory motion
	var oscPerNb;
	var oscCenRadNb, oscCenDegNb, oscCenLabel;
	var oscWidthRadNb, oscWidthDegNb, oscWidthLabel;
	var invChk;

	// views
	var rotView, cycView, oscView, invView;

	*new { |pv, whichRotation = \rotate, mode = \rtt, units = \radians, color, parent, bounds|
		^super.new(parent, bounds).init(pv, mode, whichRotation, units, color);
	}

	init { |argPv, argMode, argWhichRot, argUnits, color|

		pv = argPv;
		mode = argMode;
		units = argUnits;

		pv.addDependant(this);
		this.onClose_({ pv.removeDependant(this) });

		// whichRot always stored as either rotate, tilt, or tumble
		whichRot = switch (argWhichRot,
			\rotate, { \rotate },
			\yaw,    { \rotate },
			\tilt,   { \tilt },
			\roll,   { \tilt },
			\tumble, { \tumble },
			\pitch,  { \tumble }
		);

		// primary spec
		specRad = ControlSpec(-2pi, 2pi);
		specDeg = ControlSpec(-360, 360);

		// separate spec for Sliders, always Radian
		slSpec = switch (whichRot,
			\rotate, { [pi, pi.neg] },
			\tilt,   { [pi.neg, pi] },
			\tumble, { [pi.neg, pi] / 2 }
		).asSpec;

		this.initWidgets;
		this.mode_(mode);
		this.layItOut;
		this.units_(units);
		color !? { this.background_(color) };
	}

	mode_ { |rttOrYpr|
		var str;

		mode = rttOrYpr.asSymbol;

		str = switch (mode,
			\rtt, {
				str = whichRot.asString;
				str[0].toUpper ++ str.drop(1);
			},
			\ypr, {
				switch (whichRot,
					\rotate, {"Yaw"},
					\tilt,   {"Roll"},
					\tumble, {"Pitch"},
				)
			}
		);

		slLabel.string_(str);

		this.changed(\mode, mode);
	}

	initWidgets {

		// primary rotation
		sl = Slider()
		.action_({ |sl|
			var val = slSpec.map(sl.value);
			var method = (whichRot ++ \_).asSymbol;
			pv.perform(method, val);
			// rotRadNb.value_(val / pi);
			// rotDegNb.value_(val.raddeg);
		})
		.orientation_(\horizontal)
		.minWidth_(150).maxHeight_(20)
		;

		// slider labels, set in .mode_
		slLabel  = StaticText();
		slLabelL = StaticText();
		slLabelR = StaticText();

		rotRadNb = NumberBox()
		.action_({ |nb|
			var val = nb.value * pi;
			var method = (whichRot ++ \_).asSymbol;
			pv.perform(method, val);
			// sl.value_(
			// 	slSpec.unmap(
			// 		val.wrap(slSpec.minval, slSpec.maxval)
			// 	)
			// );
			// rotDegNb.value_(val.raddeg);
			// this.changed(\rotation, val);
		})
		.clipLo_(specRad.minval / pi).clipHi_(specRad.maxval / pi)
		.step_(0.02).scroll_step_(0.02)
		.fixedWidth_("-2.00".bounds.width * 1.2)
		.align_(\center).decimals_(2)
		;

		rotDegNb = NumberBox()
		.action_({ |nb|
			var method = (whichRot ++ \_).asSymbol;
			pv.perform(method, nb.value.degrad);
			// sl.value_(
			// 	slSpec.unmap(
			// 		nb.value.wrap(slSpec.minval, slSpec.maxval)
			// 	)
			// );
			// rotRadNb.value_(nb.value.degrad / pi);
			// this.changed(\rotation, nb.value.degrad);
		})
		.clipLo_(specDeg.minval).clipHi_(specDeg.maxval)
		.step_(0.2).scroll_step_(0.2)
		.fixedWidth_("-360.0".bounds.width * 1.2)
		.align_(\center).decimals_(1)
		;

		rotLabel = StaticText().string_("π"); // string set in .mode_

		// cyclic motion
		cycPerNb = NumberBox()
		.action_({ |nb|
			var method = (whichRot ++ \Period_).asSymbol;
			pv.perform(method, nb.value);
			// this.changed(\cyclePeriod, nb.value);
		})
		.fixedWidth_("150.0".bounds.width * 1.2)
		.clipLo_(0.01)
		.align_(\center).decimals_(2)
		;

		// oscillatory motion
		oscPerNb = NumberBox()
		.action_({ |nb|
			var method = (whichRot ++ \OscPeriod_).asSymbol;
			pv.perform(method, nb.value);
			// this.changed(\oscPeriod, nb.value);
		})
		.fixedWidth_("150.0".bounds.width * 1.2)
		.clipLo_(0.01)
		.align_(\center).decimals_(2)
		;

		oscCenRadNb = NumberBox()
		.action_({ |nb|
			var method = (whichRot ++ \OscCenter_).asSymbol;
			pv.perform(method, nb.value * pi);
			// this.changed(\oscCen, val);
			// oscCenDegNb.value = val.raddeg;
		})
		.clipLo_(specRad.minval / pi).clipHi_(specRad.maxval / pi)
		.step_(0.02).scroll_step_(0.02)
		.fixedWidth_("-2.00".bounds.width * 1.2)
		.align_(\center).decimals_(2)
		;

		oscCenDegNb = NumberBox()
		.action_({ |nb|
			var method = (whichRot ++ \OscCenter_).asSymbol;
			pv.perform(method, nb.value.degrad);
			// this.changed(\oscCen, val);
			// oscCenRadNb.value_(val / pi);
		})
		.clipLo_(specDeg.minval).clipHi_(specDeg.maxval)
		.step_(0.2).scroll_step_(0.2)
		.fixedWidth_("-360.0".bounds.width * 1.2)
		.align_(\center).decimals_(1)
		;

		oscCenLabel = StaticText().string_("π").align_(\center);

		oscWidthRadNb = NumberBox()
		.action_({ |nb|
			var method = (whichRot ++ \OscWidth_).asSymbol;
			pv.perform(method, nb.value * pi);
			// this.changed(\oscWidth, val);
			// oscWidthDegNb.value = val.raddeg;
		})
		.clipLo_(0).clipHi_(specRad.maxval / pi)
		.step_(0.02).scroll_step_(0.02)
		.fixedWidth_("-2.00".bounds.width * 1.2)
		.align_(\center).decimals_(2)
		;

		oscWidthDegNb = NumberBox()
		.action_({ |nb|
			var method = (whichRot ++ \OscWidth_).asSymbol;
			pv.perform(method, nb.value.degrad);
			// this.changed(\oscWidth, val);
			// oscWidthRadNb.value_(val / pi);
		})
		.clipLo_(0).clipHi_(specDeg.maxval)
		.step_(0.2).scroll_step_(0.2)
		.fixedWidth_("360.0".bounds.width * 1.2)
		.align_(\center).decimals_(1)
		;

		oscWidthLabel = StaticText().string_("π");

		invChk = CheckBox()
		.action_({ |cb|
			var method = (whichRot ++ \Dir_).asSymbol;
			pv.perform(method, if(cb.value, { -1 }, { 1 }));
			// this.changed(\cycleInvert, cb.value.asBoolean)
		})
		;

		[rotLabel, oscCenLabel, oscWidthLabel].do{ |str|
			str.align_(\left).stringColor_(Color.grey)
		};

		[slLabelL, slLabelR].do(_.stringColor_(Color.grey));
	}

	background_ { |color|
		[rotView, cycView, oscView, invView].do(_.background_(color));
	}
	background { ^rotView.background }

	units_ { |radiansOrDegrees|
		units = radiansOrDegrees;
		switch (units,
			\degrees, {
				[rotDegNb, oscCenDegNb, oscWidthDegNb].do(_.visible_(true));
				[rotRadNb, oscCenRadNb, oscWidthRadNb].do(_.visible_(false));
				[rotLabel, oscCenLabel, oscWidthLabel].do(_.string_("deg"));
				slLabelL.string_(slSpec.minval.raddeg.asInt.asString);
				slLabelR.string_(slSpec.maxval.raddeg.asInt.asString);
			},
			\radians, {
				[rotRadNb, oscCenRadNb, oscWidthRadNb].do(_.visible_(true));
				[rotDegNb, oscCenDegNb, oscWidthDegNb].do(_.visible_(false));
				[rotLabel, oscCenLabel, oscWidthLabel].do(_.string_("π"));
				slLabelL.string_((slSpec.minval / pi).round(0.1).asString ++ "π");
				slLabelR.string_((slSpec.maxval / pi).round(0.1).asString ++ "π");
			}
		);

	}

	layItOut {
		rotView = View().fixedHeight_(60).fixedWidth_(235);
		cycView = View().fixedHeight_(60).fixedWidth_(95);
		oscView = View().fixedHeight_(60).fixedWidth_(210);
		invView = View().fixedHeight_(60).fixedWidth_(30);

		// rotate view
		rotView.layout_(
			HLayout(
				VLayout( // slider + labels layout
					HLayout(
						slLabelL, nil, slLabel, nil, slLabelR
					),
					sl
				),
				rotRadNb, rotDegNb, // only one visible at a time
				rotLabel,
				nil
			)
		);

		// cycle view
		cycView.layout_(
			HLayout(
				cycPerNb,
				StaticText().string_("s").align_(\left).stringColor_(Color.grey),
				nil
			)
		);

		// oscillation view
		oscView.layout_(
			HLayout(
				oscPerNb,
				StaticText().string_("s").align_(\left).stringColor_(Color.grey),
				oscCenRadNb, oscCenDegNb,     // only one visible at a time
				oscCenLabel,
				oscWidthRadNb, oscWidthDegNb, // only one visible at a time
				oscWidthLabel,
				nil
			)
		);

			// motion invert view
			// cycle view
		invView.layout_(
			HLayout(
				[invChk, a: \center]
			)
		);

		[rotView, cycView, oscView, invView].do({ |me|
			me.layout.margins_([10,5,10,5])
		});

		this.layout_(
			HLayout(
				rotView, cycView, oscView, invView, nil
			).margins_(0).spacing_(2)
		);
	}

	update {
		| who, what ... args |
		var val;

		case
		{who == pv} {
			if (what == whichRot) { // rotation update
				postf("% rotation changed: %\n", whichRot, args[0]);
				val = args[0];
				sl.value_(slSpec.unmap(val.wrap(slSpec.minval, slSpec.maxval*1.0001)));
				rotRadNb.value = val / pi;
				rotDegNb.value = val.raddeg;
				^this
			};

			if (args[0] == whichRot) {
				postf("change on %, %: %\n", whichRot, what, args[1]);
				switch (what,
					\dir, {
						invChk.value_(args[1].asBoolean.not)
					},
					\rate, {
						cycPerNb.value_(args[1].reciprocal)
					},
					\auto, {

					},
					\osc, {

					},
					\oscPeriod, {
						oscPerNb.value_(args[1]);
					},
					\oscCenter, {
						oscCenRadNb.value_(args[1] / pi);
						oscCenDegNb.value_(args[1].raddeg);
					},
					\oscWidth, {
						oscWidthRadNb.value_(args[1] / pi);
						oscWidthDegNb.value_(args[1].raddeg);
					},
				)
			}
		}
	}
}
/*

Usage
 |whichRot = \rotate, mode = \rtt, parent, bounds = (Rect(0,0, 600, 200))|
v = PointViewMotionCtl( \tilt, \rtt, \radians, Color.green.alpha_(0.5) ).front
v.background_()
v.units_(\degrees)
v.units_(\radians)
v.mode_(\ypr)
v.mode_(\rtt)

v.children.do({|me|me.bounds.postln})

v = RotateControlView( \rotate, \rtt, \radians, Color.red.alpha_(0.5) ).front
v = RotateControlView( \tumble, \rtt, \radians, Color.blue.alpha_(0.5) ).front
*/