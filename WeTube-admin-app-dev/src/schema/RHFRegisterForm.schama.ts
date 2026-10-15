import * as Yup from "yup";

export const RHFRegisterFormSchema = Yup.object({
  name: Yup.string().required("Full name is required"),
  email: Yup.string().email("Invalid email").required("Email is required"),
  phone: Yup.string().required("Phone is required"),
  gender: Yup.string().oneOf(["male", "female", "other"]).required("Gender is required"),
  password: Yup.string()
    .min(6, "Password must be at least 6 characters")
    .matches(
      /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$/,
      "Password must contain at least 1 uppercase, 1 lowercase and 1 number"
    )
    .required("Password is required"),
  confirmPassword: Yup.string()
    .oneOf([Yup.ref("password")], "Passwords do not match")
    .required("Confirm Password is required"),
  dateOfBirth: Yup.string().required("Date of birth is required"),
  address: Yup.string().required("Address is required"),
  role: Yup.string().oneOf(["Manager", "Admin"]).required("Role is required"),
  status: Yup.mixed<'Pending' | 'Approved' | 'Rejected' | 'Banned'>()
    .oneOf(['Pending', 'Approved', 'Rejected', 'Banned'], "Invalid status")
    .required("Status is required"),
});