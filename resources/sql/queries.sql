-- name: users
-- Return all users.
SELECT USERNAME
FROM USERS

-- name: matched-user
-- Return matched user.
SELECT USERNAME
FROM USERS
WHERE USERNAME=:username
AND SECRET=:secret

-- name: create-user
-- Create a new user.
INSERT INTO USERS (USERNAME, SECRET)
VALUES (:username, :secret)
RETURNING USERNAME


-- name: create-weekly-report
-- Create a weekly report.
INSERT INTO WEEKVIEW (USERNAME, WEEKDATE, CONTENT)
VALUES (:username, :weekdate, :content)
RETURNING *

-- name: update-weekly-report
-- Update a weekly report.
UPDATE WEEKVIEW
SET CONTENT=:content
WHERE USERNAME=:username
AND WEEKDATE=:weekdate
RETURNING *

-- name: check-weekly-report
-- Check this existence of a weekly report.
SELECT *
FROM WEEKVIEW
WHERE USERNAME=:username
AND WEEKDATE=:weekdate
