// RotateControlView manages the controls for various rotation
// parameters used by PointView.
// State changes are broadcast from the UI and captured in
// the update method of the PointView, which is its dependent.
RotateControlView : View {

	var mode, which, units, specRad, specDeg, slSpec;

	// ui elements
	// primary rotation
	var sl, slLabel, slLabelL, slLabelR, rotRadNb, rotDegNb, rotLabel;
	// cyclic motion
	var cycPerNb, cycInvChk;
	// oscillatory motion
	var oscPerNb, oscInvChk;
	var oscCenRadNb, oscCenDegNb, oscCenLabel;
	var oscWidthRadNb, oscWidthDegNb, oscWidthLabel;

	// views
	var rotView, cycView, oscView;

	*new { |which = \rotate, mode = \rtt, units = \radians, color, parent, bounds|
		^super.new(parent, bounds).init(mode, which, units, color);
	}

	init { |argMode, argWhich, argUnits, color|

		mode = argMode;
		units = argUnits;

		// which always stored as either rotate, tilt, or tumble
		which = switch(argWhich,
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
		slSpec = switch(which,
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
				str = which.asString;
				str[0].toUpper ++ str.drop(1);
			},
			\ypr, {
				switch (which,
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
			this.changed(\rotation, val);
			rotRadNb.value_(val / pi);
			rotDegNb.value_(val.raddeg);
		})
		.orientation_(\horizontal)
		.minWidth_(150).maxHeight_(20)
		;

		slLabel = StaticText().string_(""); // string set in .mode_
		slLabelL = StaticText().string_("");
		slLabelR = StaticText().string_("");

		rotRadNb = NumberBox()
		.action_({ |nb|
			var val = nb.value * pi;
			sl.value_(
				slSpec.unmap(
					val.wrap(slSpec.minval, slSpec.maxval)
				)
			);
			rotDegNb.value_(val.raddeg);
			this.changed(\rotation, val);
		})
		.clipLo_(specRad.minval / pi).clipHi_(specRad.maxval / pi)
		.step_(0.02).scroll_step_(0.02)
		.fixedWidth_("-2.00".bounds.width * 1.2)
		.align_(\center).decimals_(2)
		;

		rotDegNb = NumberBox()
		.action_({ |nb|
			sl.value_(
				slSpec.unmap(
					nb.value.wrap(slSpec.minval, slSpec.maxval)
				)
			);
			rotRadNb.value_(nb.value.degrad / pi);
			this.changed(\rotation, nb.value.degrad);
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
			this.changed(\cyclePeriod, nb.value);
		})
		.fixedWidth_("150.0".bounds.width * 1.2)
		.clipLo_(0.01)
		.align_(\center).decimals_(2)
		;

		cycInvChk = CheckBox()
		.action_({ |cb|
			this.changed(\cycleInvert, cb.value.asBoolean)
		})
		;

		// oscillatory motion
		oscPerNb = NumberBox()
		.action_({ |nb|
			this.changed(\oscPeriod, nb.value);
		})
		.fixedWidth_("150.0".bounds.width * 1.2)
		.clipLo_(0.01)
		.align_(\center).decimals_(2)
		;

		oscCenRadNb = NumberBox()
		.action_({ |nb|
			var val = nb.value * pi;
			this.changed(\oscCen, val);
			oscCenDegNb.value = val.raddeg;
		})
		.clipLo_(specRad.minval / pi).clipHi_(specRad.maxval / pi)
		.step_(0.02).scroll_step_(0.02)
		.fixedWidth_("-2.00".bounds.width * 1.2)
		.align_(\center).decimals_(2)
		;

		oscCenDegNb = NumberBox()
		.action_({ |nb|
			var val = nb.value.degrad;
			this.changed(\oscCen, val);
			oscCenRadNb.value_(val / pi);
		})
		.clipLo_(specDeg.minval).clipHi_(specDeg.maxval)
		.step_(0.2).scroll_step_(0.2)
		.fixedWidth_("-360.0".bounds.width * 1.2)
		.align_(\center).decimals_(1)
		;

		oscCenLabel = StaticText().string_("π").align_(\center);

		oscWidthRadNb = NumberBox()
		.action_({ |nb|
			var val = nb.value * pi;
			this.changed(\oscWidth, val);
			oscWidthDegNb.value = val.raddeg;
		})
		.clipLo_(0).clipHi_(specRad.maxval / pi)
		.step_(0.02).scroll_step_(0.02)
		.fixedWidth_("-2.00".bounds.width * 1.2)
		.align_(\center).decimals_(2)
		;

		oscWidthDegNb = NumberBox()
		.action_({ |nb|
			var val = nb.value.degrad;
			this.changed(\oscWidth, val);
			oscWidthRadNb.value_(val / pi);
		})
		.clipLo_(0).clipHi_(specDeg.maxval)
		.step_(0.2).scroll_step_(0.2)
		.fixedWidth_("360.0".bounds.width * 1.2)
		.align_(\center).decimals_(1)
		;

		oscWidthLabel = StaticText().string_("π");

		oscInvChk = CheckBox()
		.action_({ |cb|
			this.changed(\cycleInvert, cb.value.asBoolean)
		})
		;

		[rotLabel, oscCenLabel, oscWidthLabel].do{ |str|
			str.align_(\left).stringColor_(Color.grey)
		};

		[slLabelL, slLabelR].do(_.stringColor_(Color.grey));
	}

	background_ { |color|
		[rotView, cycView, oscView].do(_.background_(color));
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
		rotView = View().maxHeight_(60).maxWidth_(255);
		cycView = View().maxHeight_(60).maxWidth_(95);
		oscView = View().maxHeight_(60).maxWidth_(255);

		// rotate view
		rotView.layout_(
			HLayout(
				VLayout( // slider
					HLayout(
						slLabelL, nil, slLabel, nil, slLabelR
					),
					sl
				),
				rotRadNb, rotDegNb, // only one visible at a time
				rotLabel
			)
		);

		cycView.layout_(
			HLayout(
				cycPerNb,
				StaticText().string_("s").align_(\left).stringColor_(Color.grey),
				cycInvChk
			)
		);

		oscView.layout_(
			HLayout(
				oscPerNb,
				StaticText().string_("s").align_(\left).stringColor_(Color.grey),
				oscCenRadNb, oscCenDegNb,     // only one visible at a time
				oscCenLabel,
				oscWidthRadNb, oscWidthDegNb, // only one visible at a time
				oscWidthLabel,
				oscInvChk
			)
		);

		this.layout_(
			HLayout(
				rotView, cycView, oscView
			)
		);
	}
}

/*

Usage
 |which = \rotate, mode = \rtt, parent, bounds = (Rect(0,0, 600, 200))|
v = RotateControlView( \tilt, \rtt, \radians, Color.green.alpha_(0.5) ).front
v.background_()
v.units_(\degrees)
v.units_(\radians)
v.mode_(\ypr)
v.mode_(\rtt)

v.children.do({|me|me.bounds.postln})

v = RotateControlView( \rotate, \rtt, \radians, Color.red.alpha_(0.5) ).front
v = RotateControlView( \tumble, \rtt, \radians, Color.blue.alpha_(0.5) ).front
*/