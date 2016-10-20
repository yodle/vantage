0.4.0 / Unreleased
=================
* Refactoring version creation to improve write scalability

0.3.5 / 2016-08-23
=================
* Dry-run creates no longer try to persist requested dependencies for performance reasons.

0.3.4 / 2016-08-18
=================
* Issues page is now properly paginated
* Added navbar links
* Added tooltips for active and issues icons	

0.3.3 / 2016-08-15
=================
* Fixing issue id not being set when creating an issue

0.3.2 / 2016-08-15
=================
* Fixing slow query for getting all transitive issues of a component
* Making 'undefined' a reserved version keyword equivalent to 'unknown'

0.3.1 / 2016-08-05
=================
* Fixing bug with versions requesting latest versions

0.3.0 / 2016-08-05
=================
* Removed proprietary yodle ui assets in favor of generic bootstrap assets.
* Added endpoints to get an issue by id or get all issues
* Added local embedded mode
* Added pages to create, edit, and view a single issue
* Added a page to view and search all issues

0.2.2 / 2016-07-18
=================
* Fetching components and versions that don't exist now properly returns a 404

0.2.2 / 2016-07-07
=================
* Dry run creates now take the queue head lock to prevent database deadlock issues

0.2.1 / 2016-06-24
=================
* Issues of a component's dependencies are now transitive issues on the primary component

0.2 / 2016-06-23
=================
* Added query param to createNewComponentVersion that transiently creates a Version. In other words, it immediately goes
through the process of creating a new component version, but does not persist the changes. The primary use case for this
is for a caller to see what the end result of the creation would be without wanting to permanently alter the records.
