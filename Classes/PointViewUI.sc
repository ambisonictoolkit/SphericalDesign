// A container view for the rotation/motion
// controls for manipulating PointView.

PointViewUI : View {
	var pv; // PointView
	var mstrLayout;
	var settingView;
	var rttChk, yprChk, radChk, degChk, cycChk, oscChk;
	var indicesChk, axesChk, connChk;

	// cyclic motion
	var perNb, perSl;
	// oscillatory motion
	var oscPerNb;
	var oscCenRadNb, oscCenDegNb, oscCenLabel;
	var oscWidthRadNb, oscWidthDegNb, oscWidthLabel;
	var invAllChk, invRotateChk, invTiltChk, invTumbleChk;
	var oscWidthSl;
	var radianCtls; // Dict of controls with radian/degree units, e.g. rotate, tilt...

	var <settingWidth = 235;
	var <cycleWidth = 95;
	var <oscWidth = 210;
	var <invWidth = 30;

	*new { |pointView, bounds = (Rect(0,0, 700, 300))|
		^super.new(pointView, bounds).init(pointView);
	}

	init { |pointView|

		pv = pointView;
		pv.addDependant(this);
		this.onClose_({ pv.removeDependant(this) });

		mstrLayout = VLayout().spacing_(4);
		this.layout_(mstrLayout);
		this.resize_(5);
		this.background_(Color.green.alpha_(0.25));

		rttChk = CheckBox()
		.action_({ |cb| pv.rotateMode_(if (cb.value) { \rtt } { \ypr }) })
		;

		yprChk = CheckBox()
		.action_({ |cb| pv.rotateMode_(if (cb.value) { \ypr } { \rtt }) })
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

		// cyclic motion
		perNb = NumberBox()
		.action_({ |nb|
			[
				\rotatePeriod_, \tiltPeriod_, \tumblePeriod_,
				\rotateOscPeriod_, \tiltOscPeriod_, \tumbleOscPeriod_
			].do({ |meth|
				pv.perform(meth, nb.value);
				perSl.value = nb.value.curvelin(0.1, 30, 0, 1, 4);
			})
		})
		.fixedWidth_("150.0".bounds.width * 1.2)
		.clipLo_(0.01)
		.align_(\center).decimals_(1)
		;

		perSl = Slider()
		.action_({ |sl|
			var val;
			[
				\rotatePeriod_, \tiltPeriod_, \tumblePeriod_,
				\rotateOscPeriod_, \tiltOscPeriod_, \tumbleOscPeriod_
			].do({ |meth|
				val = sl.value.lincurve(0, 1, 0.1, 30, 4);
				pv.perform(meth, val);
				perNb.value = val;
			})
		})
		.orientation_(\horizontal)
		;

		oscWidthRadNb = NumberBox()
		.action_({ |nb|
			var val;
			[\rotateOscWidth_, \tiltOscWidth_, \tumbleOscWidth_].do({ |meth|
				val = nb.value * pi;
				pv.perform(meth, val);
				oscWidthSl.value = val.lincurve(0, 2pi, 0, 1);
				oscWidthRadNb.value = val.raddeg;
			})
		})
		.clipLo_(0).clipHi_(2)
		.step_(0.05).scroll_step_(0.05)
		.fixedWidth_("-2.00".bounds.width * 1.2)
		.align_(\center).decimals_(2)
		;

		oscWidthDegNb = NumberBox()
		.action_({ |nb|
			var val;
			[\rotateOscWidth_, \tiltOscWidth_, \tumbleOscWidth_].do({ |meth|
				val = nb.value.degrad;
				pv.perform(meth, val);
				oscWidthSl.value = val.lincurve(0, 2pi, 0, 1);
				oscWidthRadNb.value = val;
			})
		})
		.clipLo_(0).clipHi_(360)
		.step_(1).scroll_step_(1)
		.fixedWidth_("360".bounds.width * 1.2)
		.align_(\center).decimals_(0)
		;

		oscWidthSl = Slider()
		.action_({ |sl|
			var val;
			[
				\rotateOscWidth_, \tiltOscWidth_, \tumbleOscWidth_
			].do({ |meth|
				val = sl.value.lincurve(0, 1, 0, 2pi);
				pv.perform(meth, val);
				oscWidthRadNb.value = val;
				oscWidthDegNb.value = val.raddeg;
			})
		})
		.orientation_(\horizontal)
		;

		oscWidthLabel = StaticText().string_("π");

		invAllChk = CheckBox().action_({ |cb|
			[invRotateChk, invTiltChk, invTumbleChk].do(_.valueAction_(cb.value))
		})
		;
		invRotateChk = CheckBox().action_({ |cb|
			pv.rotateDir_(if (cb.value, { -1 }, { 1 }));
			if (cb.value.not) { invAllChk.value_(false) };
		})
		;
		invTiltChk = CheckBox().action_({ |cb|
			pv.tiltDir_(if (cb.value, { -1 }, { 1 }));
			if (cb.value.not) { invAllChk.value_(false) };
		})
		;
		invTumbleChk = CheckBox().action_({ |cb|
			pv.tumbleDir_(if (cb.value, { -1 }, { 1 }));
			if (cb.value.not) { invAllChk.value_(false) };
		})
		;

		radianCtls = IdentityDictionary().putPairs([
			\rotate, PointViewRadianCtl(
				this, "Rotate", \rotate_, \rotate,
				[2pi, -2pi].asSpec, \radians, pv.axisColors[0]
			),
			\tilt, PointViewRadianCtl(
				this, "Tilt", \tilt_, \tilt,
				[-pi, pi].asSpec, \radians, pv.axisColors[1]
			),
			\tumble, PointViewRadianCtl(
				this, "Tumble", \tumble_, \tumble,
				[-pi/2, pi/2].asSpec, \radians, pv.axisColors[2]
			)
		]);

		settingView = View().layout_(
			HLayout(
				// Convention / Units settings
				View().fixedWidth_(settingWidth).layout_(
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
				).background_(Color.grey.alpha_(0.2)),
				// Cycle settings
				View().fixedWidth_(cycleWidth).layout_(
					VLayout(
						StaticText().string_(""),
						HLayout(
							cycChk,
							StaticText().string_("Cycle").align_(\left),
							nil
						),
						StaticText().string_("Period").align_(\left).fixedWidth_(95),
					)
				).background_(Color.grey.alpha_(0.2)),
				// Oscillation settings
				View().fixedWidth_(oscWidth).layout_(
					VLayout(
						StaticText().string_(""),
						HLayout(
							oscChk,
							StaticText().string_("Oscillate").align_(\left),
							nil
						),
						HLayout(
							StaticText().string_("Period").align_(\left),
							nil,
							StaticText().string_("Center").align_(\left),
							nil,
							StaticText().string_("Width" ).align_(\left),
							nil
						)
					)
				).background_(Color.grey.alpha_(0.2)),
				// Direction invert settings
				View().fixedWidth_(invWidth).layout_(
					VLayout(
						nil,
						StaticText().string_("Inv").align_(\center)
					)
				).background_(Color.grey.alpha_(0.2)),
				nil // anchor views left
			).margins_(0).spacing_(2),
		);

		// [
		// 	settingView,
		// 	PointViewMotionCtl( pv, this, \rotate, \rtt, \radians, pv.axisColors[2]),
		// 	PointViewMotionCtl( pv, this, \tilt,   \rtt, \radians, pv.axisColors[0]),
		// 	PointViewMotionCtl( pv, this, \tumble, \rtt, \radians, pv.axisColors[1]),
		// 	PointViewMotionCtl( pv, this, \all,    \rtt, \radians, Color.clear),
		// 	nil
		// ].do(mstrLayout.add(_));
	}

	update {
		| who, what ... args |
		var val;

		case
		{who == pv} {
			switch (what,
				\rotateMode, {
					switch (args[0],
						\rtt, {
							rttChk.value = true;
							yprChk.value = false;
							radianCtls.rotate.label = "Rotate";
							radianCtls.tilt.label = "Tilt";
							radianCtls.tumble.label = "Tumble";
						},
						\ypr, {
							rttChk.value = false;
							yprChk.value = true;
							radianCtls.rotate.label = "Yaw";
							radianCtls.tilt.label = "Roll";
							radianCtls.tumble.label = "Pitch";
						}
					)
				},
				\showIndices, { indicesChk.value = args[0].asBoolean },
				\showAxes,    { axesChk.value = args[0].asBoolean },
				\showConnections, { connChk.value = args[0].asBoolean },
				\units, {
					switch (args[0],
						\radians, { radChk.value = true;  degChk.value = false },
						\degrees, { radChk.value = false; degChk.value = true }
					);
					radianCtls.keysValuesDo{ |k,v| v.units_(args[0]) };
				},
				\allAuto, {
					cycChk.value_(args[0]);
					if (args[0]) { oscChk.value = false };
				},
				\allOsc,  {
					oscChk.value_(args[0]);
					if (args[0]) { cycChk.value = false };
				},
				\rotate, { radianCtls.rotate.value_(args[0]) },
				\tilt,   { radianCtls.tilt.value_(args[0]) },
				\tumble, { radianCtls.tumble.value_(args[0]) },

				\rotateDir, {
					invRotateChk.value = args[0].asBoolean.not;
				},
				\tiltDir, {
					invTiltChk.value = args[0].asBoolean.not;
				},
				\tumbleDir, {
					invTumbleChk.value = args[0].asBoolean.not;
				},
			)
		}
	}

}


// PointViewMotionCtl manages the controls for various rotation
// parameters used by PointView, one for each axis of rotation.
// State changes are broadcast from the UI and captured in
// the update method of the PointView, which is its dependent.

// For controlling radian values with a slider and number box.
// Supports switching to "degree mode".
PointViewRadianCtl : View {

	var pv, label, setter, getter, spec, units;

	// ui elements
	var labelTxt, unitLabel, slider, numBox;

	// spec is in radians
	*new { |pv, label, setter, getter, spec, units = \radians, color, parent, bounds|
		^super.new(parent, bounds).init(pv, label, setter, getter, spec, units, color);
	}

	init { |argPv, argLabel, argSetter, argGetter, argSpec, argUnits, color|
		pv = argPv;
		label = argLabel;
		setter = argSetter;
		getter = argGetter;
		spec = argSpec;
		units = argUnits;

		pv.addDependant(this);
		this.onClose_({ pv.removeDependant(this) });

		this.resize_(5);
		this.initWidgets;
		this.layItOut;
		this.units_(units);
		color !? { this.background_(color) };
	}

	initWidgets {
		labelTxt = StaticText().string_(label);

		unitLabel = StaticText().string_(
			switch (units, \degree, { "deg" }, \radian, { "π" })
		)
		;

		slider = Slider()
		.action_({ |sl|
			var val = spec.map(sl.value);
			switch (units,
				\degree, {
					pv.perform(setter, val.degrad);
				},
				\radian, {
					pv.perform(setter, val);
				}
			)
		})
		.orientation_(\horizontal)
		;

		numBox = NumberBox()
		.action_({ |nb|
			switch (units,
				\degree, {
					pv.perform(setter, nb.value.degrad);
				},
				\radian, {
					pv.perform(setter, (nb * pi).value);
				}
			)
		})
		.clipLo_(spec.minval / pi).clipHi_(spec.maxval / pi)
		.step_(1).scroll_step_(1)
		.fixedWidth_("-360.0".bounds.width * 1.2)
		.align_(\center)
		.decimals_(2)
		;
	}

	layItOut {
		this.layout_(
			HLayout(
				VLayout(
					HLayout(labelTxt, nil, numBox),
					slider
				),
				VLayout(unitLabel, nil)
			)
		)
	}

	units_ { |radOrDeg|
		var u = switch (radOrDeg,
			\degrees, { \degree },
			\degree,  { \degree },
			\deg,     { \degree },
			\radians, { \radian },
			\radian,  { \radian },
			\rad,     { \radian }
		);

		units = u;

		switch (units,
			\degree, {
				unitLabel.string_("deg");
				numBox
				.decimals_(1)
				.clipLo_(spec.minval.raddeg)
				.clipHi_(spec.maxval.raddeg)
				;
			},
			\radian, {
				unitLabel.string_("π");
				numBox
				.decimals_(2)
				.clipLo_(spec.minval / pi)
				.clipHi_(spec.maxval / pi)
				;
			}
		);
	}

	label_ { |string|
		labelTxt.string_(string);
	}

	// for updating the slider and numberbox together
	value_ { |rad|
		numBox.value = switch (units,
			\degree, { rad.raddeg },
			\radian, { rad / pi }
		);

		slider.value_(spec.unmap(rad));
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