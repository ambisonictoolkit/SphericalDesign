The Spherical Design Library : Read Me
========================
_A SuperCollider Quark to encapsulate a set of points which represent a
spherical design, allowing for searching, basic transformations and
visualization of the design._

#### Spherical designs
>Let *X* be a finite subset of *S<sup>(n−1)</sup>*. Spherical codes
and spherical designs are nothing but finite subsets of *S<sup>(n−1)</sup>*.
Roughly speaking, the code theoretical viewpoint is to try to find *X*,
whose points are scattered on *S<sup>(n−1)</sup>* as far as possible, i.e. the
minimum distance *d*<sub>min</sub> of *X* is as large as possible for a given
size of *X*. ... On the other hand, the design theoretical viewpoint is to try
to find *X* which globally approximates the sphere *S<sup>(n−1)</sup>*
very well. <sup>[1](#foot1)</sup>

This library does not strictly enforce the definition of a spherical design, in
fact it is simply a collection of points (`Cartesian` objects) which
you can set arbitrarily. The expectation however is that you'll be importing
designs that are provided, such as those *t*-designs available through
the subclass `TDesign`, or assigning your own points to be
regarded as a spherical design.

Once your design is initialized, you can perform basic manipulations such as
rotation, mirroring, and sorting/selection of points in the design.
:crystal_ball:


Installing
==========

Install via SuperCollider's command line:

>`Quarks.install("https://github.com/ambisonictoolkit/SphericalDesign")`

This will also install the
[PointView](https://github.com/ambisonictoolkit/PointView) Quark, which is used
to visualize the design.



Feedback and Bug Reports
========================

Known issues are logged at
[GitHub](https://github.com/ambisonictoolkit/SphericalDesign/issues).



Credits
=======

The method of formulating a triangulation of points, found in `-calcTriplets`
and its associated methods were copied, with some modification, from Scott
Wilson's port (`VBAPSpeakerArray`) of Ville Pukki's VBAP library in PureData.
See [extSphericalDesign.sc](Classes/extSphericalDesign.sc?raw=true).
&nbsp;

The T-Designs found here are from the work of Hardin and Sloane.
<sup>[2](#foot2)</sup> These and other designs can be downloaded directly from
their site:  http://neilsloane.com/sphdesigns/. If you use any of these designs,
please acknowledge this source.
&nbsp;

The development of the Spherical Design Library for SuperCollider3 is supported
by
[The University of Washington's Center for Digital Arts and Experimental Media (DXARTS)](https://dxarts.washington.edu/).
&nbsp;

Copyright the ATK Community, Joseph Anderson, and Michael McCrea, 2018.

* J Anderson : [[e-mail]](mailto:joanders[at]uw.edu)
* M McCrea : [[e-mail]](mailto:mtm5[at]uw.edu)


Contributors
------------

*  Michael McCrea (@mtmccrea)
*  Joseph Anderson (@joslloand)
*  Daniel Peterson (@dmartinp)

&nbsp;

<a name="foot1">[1]</a> Bannai, E., Bannai, E. *A survey on spherical designs
and algebraic combinatorics on spheres.* European Journal of Combinatorics,
Volume 30, Issue 6, August 2009, Pages 1392-1425.

<a name="foot2">[2]</a> R. H. Hardin and N. J. A. Sloane, *McLaren's Improved
Snub Cube and Other New Spherical Designs in Three Dimensions.* Discrete and
Computational Geometry, 15 (1996), pp. 429-441.
