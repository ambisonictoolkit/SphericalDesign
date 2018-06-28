// A container view for the rotation/motion
// controls for manipulating PointView.

PointViewUI : View {
	var pv; // PointView
	var mstrLayout;
	var settingView, labelView;
	var rttChk, yprChk, radChk, degChk, cycChk, oscChk;
	var indicesChk, axesChk, connChk;

	*new { |pointView, bounds = (Rect(0,0, 700, 300))|
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

		rttChk = CheckBox()
		.action_({ |cb| pv.rotateMode_(if (cb.value) { \rtt } { \ypr }) })
		;

		yprChk = CheckBox()
		.action_({ |cb|
			pv.rotateMode_(if (cb.value) { \ypr } { \rtt })
		})
		;

		radChk = CheckBox()
		.action_({ |cb| pv.units_(if (cb.value) { \radians } { \degrees }) })
		;

		degChk = CheckBox()
		.action_({ |cb| pv.units_(if (cb.value) { \degrees } { \radians }) })
		;

		cycChk = CheckBox()
		.action_({ |cb| pv.allAuto_(cb.value) })
		;

		oscChk = CheckBox()
		.action_({ |cb| pv.allOsc_(cb.value) })
		;

		indicesChk = CheckBox()
		.action_({ |cb| pv.showIndices_(cb.value) })
		;
		axesChk = CheckBox()
		.action_({ |cb| pv.showAxes_(cb.value) })
		;
		connChk = CheckBox()
		.action_({ |cb| pv.showConnections_(cb.value) })
		;

		settingView = View().layout_(
			HLayout(
				View().layout_(
					HLayout(
						VLayout(
							StaticText().string_("Convention"),
							HLayout(
								rttChk, StaticText().string_("RTT"), nil
							),
							HLayout(
								yprChk, StaticText().string_("YPR"), nil
							)
						),
						VLayout(
							StaticText().string_("Units"),
							HLayout(
								radChk, StaticText().string_("Radians"), nil
							),
							HLayout(
								degChk, StaticText().string_("Degrees"), nil
							)
						)
					).margins_(0)
				).fixedWidth_(235),
				cycChk,
				StaticText().string_("Cycle"),
				40,
				oscChk,
				StaticText().string_("Oscillate"),
				nil
			)
		);

		labelView = View().layout_(
			HLayout(
				View().layout_(
					// HLayout(
					// 	nil,
					// 	cycChk,
					// 	StaticText().string_("Cycle"),
					// 	oscChk,
					// 	StaticText().string_("Oscillate"),
					// 	15
					// )
				).fixedWidth_(235),
				StaticText().string_("Period").align_(\left).fixedWidth_(95),
				View().layout_(
					HLayout(
						StaticText().string_("Period").align_(\left),
						StaticText().string_("Center").align_(\left),
						StaticText().string_("Width").align_(\left),
						nil
					).margins_(0).spacing_(20)
				).fixedWidth_(205),
				StaticText().string_("Inv").align_(\left),
			).spacing_(2)
		);

		[
			settingView,
			labelView,
			PointViewMotionCtl( pv, \rotate, \rtt, \radians, pv.axisColors[2]),
			PointViewMotionCtl( pv, \tilt,   \rtt, \radians, pv.axisColors[0]),
			PointViewMotionCtl( pv, \tumble, \rtt, \radians, pv.axisColors[1]),
			PointViewMotionCtl( pv, \all,    \rtt, \radians, Color.clear),
			nil
		].do(mstrLayout.add(_));
	}

	update {
		| who, what ... args |
		var val;

		case
		{who == pv} {
			switch (what,
				\rotateMode, {
					switch (args[0],
						\rtt, { rttChk.value = true; yprChk.value = false },
						\ypr, { rttChk.value = false; yprChk.value = true }
					)
				},
				\showIndices, { indicesChk.value = args[0].asBoolean },
				\showAxes, { axesChk.value = args[0].asBoolean },
				\showConnections, { connChk.value = args[0].asBoolean },
				\units, {
					switch (args[0],
						\radians, { radChk.value = true; degChk.value = false },
						\degrees, { radChk.value = false; degChk.value = true }
					)
				},
				\allAuto, {
					cycChk.value_(args[0]);
					if (args[0]) { oscChk.value = false };
				},
				\allOsc,  {
					oscChk.value_(args[0]);
					if (args[0]) { cycChk.value = false };
				},
			)
		}
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
			\pitch,  { \tumble },
			\all,    { \all }
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

		if (whichRot != \all) {

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
		};
	}

	initWidgets {

		// primary rotation
		sl = Slider()
		.action_({ |sl|
			var val = slSpec.map(sl.value);
			var method = (whichRot ++ \_).asSymbol;
			pv.perform(method, val);
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
		})
		.fixedWidth_("150.0".bounds.width * 1.2)
		.clipLo_(0.01)
		.align_(\center).decimals_(1)
		;

		// oscillatory motion
		oscPerNb = NumberBox()
		.action_({ |nb|
			var method = (whichRot ++ \OscPeriod_).asSymbol;
			pv.perform(method, nb.value);
		})
		.fixedWidth_("150.0".bounds.width * 1.2)
		.clipLo_(0.01)
		.align_(\center).decimals_(1)
		;

		oscCenRadNb = NumberBox()
		.action_({ |nb|
			var method = (whichRot ++ \OscCenter_).asSymbol;
			pv.perform(method, nb.value * pi);
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
				[oscCenDegNb, oscWidthDegNb].do(_.visible_(true));
				[oscCenRadNb, oscWidthRadNb].do(_.visible_(false));
				[oscCenLabel, oscWidthLabel].do(_.string_("deg"));

				if (whichRot != \all)  {
					rotDegNb.visible = true;
					rotRadNb.visible = false;
					rotLabel.string_("deg");
					slLabelL.string_(slSpec.minval.raddeg.asInt.asString);
					slLabelR.string_(slSpec.maxval.raddeg.asInt.asString);
				};
			},
			\radians, {
				[oscCenRadNb, oscWidthRadNb].do(_.visible_(true));
				[oscCenDegNb, oscWidthDegNb].do(_.visible_(false));
				[oscCenLabel, oscWidthLabel].do(_.string_("π"));

				if (whichRot != \all)  {
					rotRadNb.visible = true;
					rotDegNb.visible = false;
					rotLabel.string_("π");
					slLabelL.string_((slSpec.minval / pi).round(0.1).asString ++ "π");
					slLabelR.string_((slSpec.maxval / pi).round(0.1).asString ++ "π");
				};
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
			if (whichRot == \all)  {
				HLayout(nil, StaticText().string_("All").align_(\right))
			} {
				HLayout(
					VLayout( // slider + labels layout
						HLayout(slLabelL, nil, slLabel, nil, slLabelR),
						sl
					),
					rotRadNb, rotDegNb, // only one visible at a time
					rotLabel,
					nil
				)
			}
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
			// if (what == whichRot) { // rotation value update
			// 	val = args[0];
			// 	sl.value_(slSpec.unmap(val.wrap(slSpec.minval, slSpec.maxval*1.0001)));
			// 	rotRadNb.value = val / pi;
			// 	rotDegNb.value = val.raddeg;
			// 	^this // break
			// };
			// if (what == \units) {
			// 	this.units_(args[0]);
			// 	^this // break
			// };

			switch (what,
				whichRot, { // rotation value update
					val = args[0];
					sl.value_(slSpec.unmap(val.wrap(slSpec.minval, slSpec.maxval*1.0001)));
					rotRadNb.value = val / pi;
					rotDegNb.value = val.raddeg;
					^this // break
				},
				\units, {
					this.units_(args[0]);
					^this // break
				},
				\rotateMode, {
					this.mode_(args[0]);
					^this // break
				}
			);

			if (args[0] == whichRot) {
				switch (what,
					\dir,  { invChk.value_(args[1].asBoolean.not) },
					\rate, { cycPerNb.value_(args[1].reciprocal) },
					\auto, { },
					\osc,  { },
					\oscPeriod, { oscPerNb.value_(args[1]) },
					\oscCenter, {
						oscCenRadNb.value_(args[1] / pi);
						oscCenDegNb.value_(args[1].raddeg);
					},
					\oscWidth, {
						oscWidthRadNb.value_(args[1] / pi);
						oscWidthDegNb.value_(args[1].raddeg);
					}
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