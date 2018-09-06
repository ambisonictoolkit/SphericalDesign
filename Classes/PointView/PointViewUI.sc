// A container view for the rotation/motion
// controls for manipulating PointView.

PointViewUI : View {
	var pv; // PointView
	var mstrLayout;
	var conventionView, oscView, whichCycView, whichOscView, rttView, varyMotionView, periodView, widthView;
	var rttChk, yprChk, radChk, degChk, cycChk, oscChk;
	var indicesChk, axesChk, connChk, resetBut;
	var units = \degrees;

	// cyclic motion
	var perNb, perSl;
	// oscillatory motion
	var oscPerNb;
	var oscCenRadNb, oscCenDegNb, oscCenLabel;
	var oscWidthRadNb, oscWidthDegNb, oscWidthLabel;
	var cycAllChk, cycRotateChk, cycTiltChk, cycTumbleChk;
	var oscAllChk, oscRotateChk, oscTiltChk, oscTumbleChk;
	var varyAllChk, varyRotateChk, varyTiltChk, varyTumbleChk;
	var invAllChk, invRotateChk, invTiltChk, invTumbleChk;
	var oscWidthSl;
	var <radianCtls; // Dict of controls with radian/degree units, e.g. rotate, tilt...

	*new { |pointView, bounds = (Rect(0,0, 420, 530))|
		^super.new(bounds: bounds).init(pointView);
	}

	init { |pointView|
		pv = pointView;

		pv.addDependant(this);
		this.onClose_({ pv.removeDependant(this) });

		mstrLayout = VLayout().spacing_(4);
		this.layout_(mstrLayout);
		// this.resize_(5);

		this.initWidgets;

		pv.units_(units);
		this.layItOut;

		// hide motion views initially
		[whichCycView, whichOscView, periodView, widthView, varyMotionView].do(_.visible_(false));

	}

	initWidgets {
		var nbWidth = "-360.0".bounds.width * 1.2;

		// Settings check boxes
		rttChk = CheckBox()
		.action_({ |cb| pv.rotateMode_(if (cb.value) { \rtt } { \ypr }) })
		.value_(pv.rotateMode == \rtt)
		;

		yprChk = CheckBox()
		.action_({ |cb| pv.rotateMode_(if (cb.value) { \ypr } { \rtt }) })
		.value_(pv.rotateMode == \ypr)
		;

		radChk = CheckBox()
		.action_({ |cb| pv.units_(if (cb.value) { \radians } { \degrees }) })
		;
		degChk = CheckBox()
		.action_({ |cb| pv.units_(if (cb.value) { \degrees } { \radians }) })
		;

		// Master motion check boxes
		cycChk = CheckBox()
		.action_({ |cb|
			var val = cb.value;

			if (val) {
				// toggle oscillate off
				oscChk.value_(false);
				pv.rotateOsc_(false);
				pv.tiltOsc_(false);
				pv.tumbleOsc_(false);
				// send cycle states of each axis
				[cycRotateChk, cycTiltChk, cycTumbleChk].do({ |me|
					me.valueAction_(me.value)
				});
				// hide osc-specific views (used by oscillate)
				widthView.visible_(false);
				whichOscView.visible_(false);
			} {
				pv.rotateCyc_(false);
				pv.tiltCyc_(false);
				pv.tumbleCyc_(false);
				// if disabling, reset rotation to base rotaitons
				if (oscChk.value.not) {
					pv.rotate_(pv.baseRotation).tilt_(pv.baseTilt).tumble_(pv.baseTumble);
					pv.rotatePhase_(0).tiltPhase_(0).tumblePhase_(0);
				}
			};

			[whichCycView, periodView, varyMotionView].do(_.visible_(val));

		})
		;

		oscChk = CheckBox()
		.action_({ |cb|
			var val = cb.value;

			if (val) {
				// toggle cycling off
				cycChk.value_(false);
				pv.rotateCyc_(false);
				pv.tiltCyc_(false);
				pv.tumbleCyc_(false);
				// send oscillate states of each axis
				[oscRotateChk, oscTiltChk, oscTumbleChk].do({ |me|
					me.valueAction_(me.value)
				});
				// hide cyc-specific views (used by oscillate)
				whichCycView.visible_(false);
			} {
				pv.rotateOsc_(false);
				pv.tiltOsc_(false);
				pv.tumbleOsc_(false);
				// if disabling, reset rotation to base rotaitons
				if (cycChk.value.not) {
					// reset rotation to base rotaitons
					pv.rotate_(pv.baseRotation).tilt_(pv.baseTilt).tumble_(pv.baseTumble);
					pv.rotatePhase_(0).tiltPhase_(0).tumblePhase_(0);
				}
			};

			[whichOscView, periodView, widthView, varyMotionView].do(_.visible_(val));

		})
		;

		// "Show" check boxes
		indicesChk = CheckBox()
		.action_({ |cb| pv.showIndices_(cb.value) })
		;
		axesChk = CheckBox()
		.action_({ |cb| pv.showAxes_(cb.value) })
		;
		connChk = CheckBox()
		.action_({ |cb| pv.showConnections_(cb.value) })
		;

		resetBut = Button()
		.action_({ |but|
			cycChk.valueAction_(false);
			oscChk.valueAction_(false);
			perNb.valueAction_(30);         // default osc/cycle period
			oscWidthDegNb.valueAction_(8);  // default osc width
			pv.reset;
		})
		.states_([["Reset"]])
		;

		// cyclic/osc motion
		perNb = NumberBox()
		.action_({ |nb|
			[
				\rotateCycPeriod_, \tiltCycPeriod_, \tumbleCycPeriod_,
				\rotateOscPeriod_, \tiltOscPeriod_, \tumbleOscPeriod_
			].do({ |meth|
				pv.perform(meth, nb.value);
				perSl.value = nb.value.curvelin(0.5, 70, 0, 1, 4);
			})
		})
		.fixedWidth_(nbWidth)
		.clipLo_(0.01)
		.align_(\center).decimals_(1)
		.value_(pv.rotatePeriod)
		;

		perSl = Slider()
		.action_({ |sl|
			var val;
			[
				\rotateCycPeriod_, \tiltCycPeriod_, \tumbleCycPeriod_,
				\rotateOscPeriod_, \tiltOscPeriod_, \tumbleOscPeriod_
			].do({ |meth|
				val = sl.value.lincurve(0, 1, 0.5, 70, 4);
				pv.perform(meth, val); // sets both osc and cyc period
				perNb.value = val;
			})
		})
		.orientation_(\horizontal)
		.fixedHeight_(25)
		.value_(pv.rotatePeriod.curvelin(0.5, 70, 0, 1, 4))
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
		.fixedWidth_(nbWidth)
		.align_(\center).decimals_(2)
		.value_(pv.rotateOscWidth / pi)
		;

		oscWidthDegNb = NumberBox()
		.action_({ |nb|
			var val;
			[\rotateOscWidth_, \tiltOscWidth_, \tumbleOscWidth_].do({ |meth|
				val = nb.value.degrad;
				pv.perform(meth, val);
				oscWidthSl.value = val.curvelin(0, 2pi, 0, 1, 4.3);
				oscWidthRadNb.value = val;
			})
		})
		.clipLo_(0).clipHi_(360)
		.step_(1).scroll_step_(1)
		.fixedWidth_(nbWidth)
		.align_(\center).decimals_(0)
		.value_(pv.rotateOscWidth.raddeg)
		;

		oscWidthSl = Slider()
		.action_({ |sl|
			var val;
			[
				\rotateOscWidth_, \tiltOscWidth_, \tumbleOscWidth_
			].do({ |meth|
				val = sl.value.lincurve(0, 1, 0, 2pi, 4.3);
				pv.perform(meth, val);
				oscWidthRadNb.value = val / pi;
				oscWidthDegNb.value = val.raddeg;
			})
		})
		.orientation_(\horizontal)
		.fixedHeight_(25)
		.value_(pv.rotateOscWidth.curvelin(0, 2pi, 0, 1, 4.3))
		;

		oscWidthLabel = StaticText()
		.string_("π").fixedWidth_("deg".bounds.width * 1.2);

		cycAllChk = CheckBox()
		.action_({ |cb|
			var val = cb.value;
			pv.allCyc_(val);
		})
		.value_([pv.cycRotate, pv.cycTilt, pv.cycTumble].every({ |me| me }))
		;
		cycRotateChk = CheckBox()
		.action_({ |cb|
			pv.rotateCyc_(cb.value);
			if (cb.value.not) { cycAllChk.value_(false) };
		})
		.value_(pv.cycRotate)
		;
		cycTiltChk = CheckBox()
		.action_({ |cb|
			pv.tiltCyc_(cb.value);
			if (cb.value.not) { cycAllChk.value_(false) };
		})
		.value_(pv.cycTilt)
		;
		cycTumbleChk = CheckBox()
		.action_({ |cb|
			pv.tumbleCyc_(cb.value);
			if (cb.value.not) { cycAllChk.value_(false) };
		})
		.value_(pv.cycTumble)
		;

		oscAllChk = CheckBox()
		.action_({ |cb|
			var val = cb.value;
			pv.allOsc_(val);
		})
		.value_([pv.oscRotate, pv.oscTilt, pv.oscTumble].every({ |me| me }))
		;
		oscRotateChk = CheckBox()
		.action_({ |cb|
			pv.rotateOsc_(cb.value);
			if (cb.value.not) { oscAllChk.value_(false) };
		})
		.value_(pv.oscRotate)
		;
		oscTiltChk = CheckBox()
		.action_({ |cb|
			pv.tiltOsc_(cb.value);
			if (cb.value.not) { oscAllChk.value_(false) };
		})
		.value_(pv.oscTilt)
		;
		oscTumbleChk = CheckBox()
		.action_({ |cb|
			pv.tumbleOsc_(cb.value);
			if (cb.value.not) { oscAllChk.value_(false) };
		})
		.value_(pv.oscTumble)
		;

		varyAllChk = CheckBox()
		.action_({ |cb|
			[varyRotateChk, varyTiltChk, varyTumbleChk].do(_.valueAction_(cb.value))
		})
		.value_([\rotate, \tilt, \tumble].collect(pv.randomizedAxes[_]).every({ |me| me }))
		;
		varyRotateChk = CheckBox()
		.action_({ |cb|
			pv.varyMotion_(\rotate, cb.value);
			if (cb.value.not) { varyAllChk.value_(false) };
		})
		.value_(pv.randomizedAxes.rotate)
		;
		varyTiltChk = CheckBox()
		.action_({ |cb|
			pv.varyMotion_(\tilt, cb.value);
			if (cb.value.not) { varyAllChk.value_(false) };
		})
		.value_(pv.randomizedAxes.tilt)
		;
		varyTumbleChk = CheckBox()
		.action_({ |cb|
			pv.varyMotion_(\tumble, cb.value);
			if (cb.value.not) { varyAllChk.value_(false) };
		})
		.value_(pv.randomizedAxes.tumble)
		;

		invAllChk = CheckBox()
		.action_({ |cb|
			[invRotateChk, invTiltChk, invTumbleChk].do(_.valueAction_(cb.value))
		})
		.value_([\rotateDir, \tiltDir, \tumbleDir].collect(pv.perform(_)).every({ |me| me < 1 }))
		;
		invRotateChk = CheckBox().action_({ |cb|
			pv.rotateDir_(if (cb.value, { -1 }, { 1 }));
			if (cb.value.not) { invAllChk.value_(false) };
		}).value_(pv.rotateDir < 1)
		;
		invTiltChk = CheckBox().action_({ |cb|
			pv.tiltDir_(if (cb.value, { -1 }, { 1 }));
			if (cb.value.not) { invAllChk.value_(false) };
		}).value_(pv.tiltDir < 1)
		;
		invTumbleChk = CheckBox().action_({ |cb|
			pv.tumbleDir_(if (cb.value, { -1 }, { 1 }));
			if (cb.value.not) { invAllChk.value_(false) };
		}).value_(pv.tumbleDir < 1)
		;

		radianCtls = IdentityDictionary(know: true).putPairs([
			\rotate, PointViewRadianCtl(
				pv, "Rotate", \rotate_, \baseRotation,
				[2pi, -2pi].asSpec, \radians, pv.axisColors[2]
			),
			\tilt, PointViewRadianCtl(
				pv, "Tilt", \tilt_, \baseTilt,
				[-pi, pi].asSpec, \radians, pv.axisColors[0]
			),
			\tumble, PointViewRadianCtl(
				pv, "Tumble", \tumble_, \baseTumble,
				[-pi/2, pi/2].asSpec, \radians, pv.axisColors[1]
			)
		]);
	}

	layItOut {
		conventionView = View().layout_(
			VLayout(
				// Convention / Units settings
				View().layout_(
					HLayout(
						StaticText().string_("Convention").font_(Font.default.bold_(true)),
						HLayout(
							rttChk, StaticText().string_("RTT"), nil
						),
						HLayout(
							yprChk, StaticText().string_("YPR"), nil
						),
						nil, nil, nil,

						StaticText().string_("Units").font_(Font.default.bold_(true)),
						HLayout(
							radChk, StaticText().string_("Radians"), nil
						),
						HLayout(
							degChk, StaticText().string_("Degrees"), nil
						)
					).margins_(0).spacing_(5)
				)
			).margins_(0)
		);

		rttView = View().layout_(
			VLayout(
				radianCtls.rotate.fixedHeight_(70),
				radianCtls.tilt.fixedHeight_(70),
				radianCtls.tumble.fixedHeight_(70),
			).margins_(0)
		)
		;

		whichCycView = View().layout_(
			HLayout(
				StaticText().string_("Which\nAxes")
				.align_(\center).fixedWidth_(55).font_(Font.default.bold_(true)),
				VLayout(
					nil,
					HLayout(
						cycAllChk,
						StaticText().string_("All").align_(\left),
						nil
					),
					HLayout(
						cycRotateChk,
						StaticText().string_("Rotate").align_(\left),
						nil,
						cycTiltChk,
						StaticText().string_("Tilt").align_(\left),
						nil,
						cycTumbleChk,
						StaticText().string_("Tumble").align_(\left),
						nil,
					),
				)
			).margins_(5).spacing_(10),
		);

		whichOscView = View().layout_(
			HLayout(
				StaticText().string_("Which\nAxes")
				.align_(\center).fixedWidth_(55).font_(Font.default.bold_(true)),
				VLayout(
					nil,
					HLayout(
						oscAllChk,
						StaticText().string_("All").align_(\left),
						nil
					),
					HLayout(
						oscRotateChk,
						StaticText().string_("Rotate").align_(\left),
						nil,
						oscTiltChk,
						StaticText().string_("Tilt").align_(\left),
						nil,
						oscTumbleChk,
						StaticText().string_("Tumble").align_(\left),
						nil,
					),
				)
			).margins_(5).spacing_(10),
		);

		varyMotionView = View().layout_(
			VLayout(
				HLayout(
					StaticText().string_("Vary\nMotion")
					.align_(\center).fixedWidth_(55).font_(Font.default.bold_(true)),
					VLayout(
						nil,
						HLayout(
							varyAllChk,
							StaticText().string_("All").align_(\left),
							nil
						),
						HLayout(
							varyRotateChk,
							StaticText().string_("Rotate").align_(\left),
							nil,
							varyTiltChk,
							StaticText().string_("Tilt").align_(\left),
							nil,
							varyTumbleChk,
							StaticText().string_("Tumble").align_(\left),
							nil,
						),
					)
				).margins_(5).spacing_(10),
				10,
				HLayout(
					StaticText().string_("Invert\nMotion")
					.align_(\center).fixedWidth_(55).font_(Font.default.bold_(true)),
					VLayout(
						nil,
						HLayout(
							invAllChk,
							StaticText().string_("All").align_(\left),
							nil
						),
						HLayout(
							invRotateChk,
							StaticText().string_("Rotate").align_(\left),
							nil,
							invTiltChk,
							StaticText().string_("Tilt").align_(\left),
							nil,
							invTumbleChk,
							StaticText().string_("Tumble").align_(\left),
							nil,
						),
					)
				).margins_(5).spacing_(10)
			).margins_(0).spacing_(2),
		)
		;

		oscView = View().layout_(
			VLayout(
				HLayout(
					oscChk,
					StaticText().string_("Oscillate").align_(\left).font_(Font.default.bold_(true)),
					15,
					cycChk,
					StaticText().string_("Cycle").align_(\left).font_(Font.default.bold_(true)),
					nil,
					resetBut
				),
				whichOscView.visible_(false),
				whichCycView.visible_(false),
				periodView = View().layout_(
					VLayout(
						HLayout(
							StaticText().string_("Period").align_(\left),
							nil,
							perNb,
							StaticText().string_("s").align_(\left).fixedWidth_("deg".bounds.width * 1.2),
						),
						perSl
					).margins_(0)
				).fixedHeight_(55),

				widthView = View().layout_(
					VLayout(
						HLayout(
							StaticText().string_("Width").align_(\left),
							nil,
							oscWidthRadNb, oscWidthDegNb, // only one visible at a time: change to one numbox with switch in action?
							oscWidthLabel
						),
						oscWidthSl
					).margins_(0)
				).fixedHeight_(55),

				varyMotionView
			)
		)
		;

		[
			conventionView,
			VLayout(rttView, oscView),
			nil
		].do(mstrLayout.add(_));
	}

	update {
		| who, what ... args |

		case
		{who == pv} {
			switch (what,
				\rotateMode, {
					switch (args[0],
						\rtt, {
							rttChk.value = true;
							yprChk.value = false;
							radianCtls.rotate.label = "Rotate";
							radianCtls.tilt.label   = "Tilt";
							radianCtls.tumble.label = "Tumble";
						},
						\ypr, {
							rttChk.value = false;
							yprChk.value = true;
							radianCtls.rotate.label = "Yaw";
							radianCtls.tilt.label   = "Roll";
							radianCtls.tumble.label = "Pitch";
						}
					)
				},
				\showIndices, { indicesChk.value = args[0].asBoolean },
				\showAxes,    { axesChk.value = args[0].asBoolean },
				\showConnections, { connChk.value = args[0].asBoolean },
				\units, {
					units = args[0];
					switch (units,
						\radians, {
							radChk.value = true;
							degChk.value = false;
							oscWidthRadNb.visible_(true);
							oscWidthDegNb.visible_(false);
							oscWidthLabel.string_("π");
						},
						\degrees, {
							radChk.value = false;
							degChk.value = true;
							oscWidthRadNb.visible_(false);
							oscWidthDegNb.visible_(true);
							oscWidthLabel.string_("deg");
						}
					);
					radianCtls.keysValuesDo{ |k,v| v.units_(args[0]) };
				},
				\allCyc, {
					var bool = args[0];
					cycAllChk.value_(bool);
					cycRotateChk.value_(bool);
					cycTiltChk.value_(bool);
					cycTumbleChk.value_(bool);
					if (args[0]) {
						oscChk.value = false;
					};
				},
				\allOsc,  {
					var bool = args[0];
					oscAllChk.value_(bool);
					oscRotateChk.value_(bool);
					oscTiltChk.value_(bool);
					oscTumbleChk.value_(bool);
					if (args[0]) {
						cycChk.value = false;
					};
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
				\axisColors, {
					// color axis order is x, y, z
					[\tilt, \tumble, \rotate].do({ |rot, i| radianCtls[rot].color_(args[0][i]) });
				}
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

	var pv, label, setter, getter, spec, units, color;
	// ui elements
	var labelTxt, unitLabel, slider, numBox;

	// spec is in radians
	*new { |pv, label, setter, getter, spec, units = \radians, color, parent, bounds|
		^super.new(parent, bounds).init(pv, label, setter, getter, spec, units, color);
	}

	init { |argPv, argLabel, argSetter, argGetter, argSpec, argUnits, argColor|
		pv = argPv;
		label = argLabel;
		setter = argSetter;
		getter = argGetter;
		spec = argSpec;
		units = argUnits;
		color = argColor;

		pv.addDependant(this);
		this.onClose_({ pv.removeDependant(this) });

		this.resize_(5);
		this.initWidgets;
		this.layItOut;
		this.units_(units);
		this.value_(pv.perform(getter));
	}

	initWidgets {
		labelTxt = StaticText().string_(label);
		color !? { labelTxt.stringColor_(color) };

		unitLabel = StaticText().string_(
			switch (units, \degrees, { "deg" }, \radians, { "π" })
		)
		.align_(\eft)
		.fixedWidth_("deg".bounds.width * 1.2)
		;

		slider = Slider()
		.action_({ |sl|
			var val = spec.map(sl.value);
			pv.perform(setter, val);
		})
		.orientation_(\horizontal)
		.fixedHeight_(25)
		;

		numBox = NumberBox()
		.action_({ |nb|
			switch (units,
				\degrees, {
					pv.perform(setter, nb.value.degrad);
				},
				\radians, {
					pv.perform(setter, (nb.value * pi).value);
				}
			)
		})
		.clipLo_(min(spec.minval, spec.maxval) / pi)
		.clipHi_(max(spec.minval, spec.maxval) / pi)
		.step_(1).scroll_step_(1)
		.fixedWidth_("-360.0".bounds.width * 1.2)
		.align_(\center)
		.decimals_(2)
		;
	}

	layItOut {
		this.layout_(
			VLayout(
				HLayout(labelTxt, nil, numBox, unitLabel),
				slider
			)
		)
	}

	units_ { |radOrDeg|
		var u = switch (radOrDeg,
			\degrees, { \degrees },
			\degree,  { \degrees },
			\deg,     { \degrees },
			\radians, { \radians },
			\radian,  { \radians },
			\rad,     { \radians }
		);

		units = u;

		switch (units,
			\degrees, {
				unitLabel.string_("deg");
				numBox
				.decimals_(1)
				.clipLo_(min(spec.minval, spec.maxval).raddeg)
				.clipHi_(max(spec.minval, spec.maxval).raddeg)
				.value_(pv.perform(getter).raddeg)
				;
			},
			\radians, {
				unitLabel.string_("π");
				numBox
				.decimals_(2)
				.clipLo_(min(spec.minval, spec.maxval) / pi)
				.clipHi_(max(spec.minval, spec.maxval) / pi)
				.value_(pv.perform(getter) / pi)
				;
			}
		);
	}

	label_ { |string|
		labelTxt.string_(string);
	}

	color_ { |aColor|
		color = aColor;
		labelTxt.stringColor_(color);
	}

	// for updating the slider and numberbox together
	value_ { |rad|
		numBox.value = switch (units,
			\degrees, { rad.raddeg },
			\radians, { rad / pi }
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