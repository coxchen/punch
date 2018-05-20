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
