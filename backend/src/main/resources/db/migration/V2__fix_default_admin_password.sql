-- Fix default admin password (admin/admin123) for existing dev DBs.
update admin_user
set password_hash = '$2a$10$Y6L4gb2LrhSeZslXp05fWeHDt3yRF.UKyQasncyZ9mMbCik43ASVm'
where username = 'admin';

