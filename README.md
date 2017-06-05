# aws-ami-trigger
Jenkins plugin to poll for new AMIs and trigger a build for the latest matching AMI.

Enter a schedule, AWS credentials and one or more AMI filters to trigger a build when a new AMI is matched.
This plugin uses the [AWS EC2 DecribeImages](https://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_DescribeImages.html) service
to find matching AMIs.

The following filters are supported:

  * name - The name of the AMI (provided during image creation - supports wildcards)
  * description - The description of the image (provided during image creation - supports wildcards)
  * tags (key=value) - The key/value combination of a tag assigned to the resource

The following advanced attributes are also supported:
  * architecture - The image architecture (i386 | x86_64)
  * is-public - A Boolean that indicates whether the image is public
  * owner-alias - String value from an Amazon-maintained list (amazon | aws-marketplace | microsoft) of snapshot owners
  * owner-id - The AWS account ID of the image owner
  * product-code - The product code

(Note that state is always set to available).

The filters are configured on the build configuration page:

![Screenshot](images/screenshot-1.png)
