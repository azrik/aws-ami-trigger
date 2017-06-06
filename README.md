# aws-ami-trigger
A Jenkins plugin to poll for new AMIs and trigger a build for the latest matching AMI.

## Overview
This plugin uses the [AWS EC2 DecribeImages](https://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_DescribeImages.html) service
to find matching AMIs.

The following filters are supported:

  * `name` - The name of the AMI (provided during image creation - supports wildcards)
  * `description` - The description of the image (provided during image creation - supports wildcards)
  * `tags` (key=value) - The key/value combination of a tag assigned to the resource

The following advanced filters are also supported:
  * `architecture` - the image architecture (`i386` | `x86_64`)
  * `is-public` - a boolean that indicates whether the image is public
  * `owner-alias` - string value from an Amazon-maintained list (`amazon` | `aws-marketplace` | `microsoft`) of snapshot owners
  * `owner-id` - the AWS account ID of the image owner
  * `product-code` - the product code

(Note that `state` is always set to `available`).

## Configuration

The filters are configured on the build configuration page:

![Screenshot](images/screenshot-1.png)

This has the following fields:

  * **Schedule** - a cron style schedule for the trigger
  * **Amazon EC2 Credentials** - the id of credentials added via the [AWS Credentials Plugin](https://plugins.jenkins.io/aws-credentials)
  * **Amazon EC2 Region Name** - the region name to search for AMIs (defaults to `us-east-1`)
  * **Name** - the name of the AMI
  * **Description** - the description of the AMI
  * **Tags** - tags for the AMI in the form `key=value[;key=value]`, for example `Project=Awesome;Role=Web`

Note that at least one of **Name**, **Description** or **Tags** must be specified to prevent the filter fetching too many AMIs.

Advanced filters may also be specified:

![Screenshot](images/screenshot-2.png)

  * **Architecture** - the image architecture (`i386` | `x86_64`)
  * **Owner Alias** - value from an Amazon-maintained list (`amazon` | `aws-marketplace` | `microsoft`) of snapshot owners
  * **Owner Id** - the AWS account ID of the image owner
  * **Product Code** - the product code
  * **Public** - a boolean that indicates whether the image is public

Click **Test Filter** to test the filter before saving. This displays the number of AMIs currently matching the filter and the top 10
latest matches. It displays the following attributes of those images: `creation-date`, `image-id`, `name` and `description`.

![Screenshot](images/screenshot-3.png)


Click **Add** to add more filters.

## Environment variables

For each build that is triggered, the following environment variable indicates how many of the filters triggered:
  * `awsAmiTriggerCount` - count of the number of filters that triggered

For each filter that triggered, the following image related environment variables are available to the build:

  * `awsAmiTriggerImageArchitecture1` - the image architecture (`i386` | `x86_64`)
  * `awsAmiTriggerImageCreationDate1` - the date and time the image was created
  * `awsAmiTriggerImageDescription1` - the description of the AMI that was provided during image creation
  * `awsAmiTriggerImageHypervisor1` - the hypervisor type of the image (`ovm` | `xen`)
  * `awsAmiTriggerImageId1` - the ID of the AMI
  * `awsAmiTriggerImageType1` - the type of image (`machine` | `kernel` | `ramdisk`)
  * `awsAmiTriggerImageName1` - the name of the AMI that was provided during image creation
  * `awsAmiTriggerOwnerAlias1` - the AWS account alias (for example, amazon, self) or the AWS account ID of the AMI owner
  * `awsAmiTriggerOwnerId1` - the AWS account ID of the image owner
  * `awsAmiTriggerImageProductCodes1` - Any product codes associated with the AMI
  * `awsAmiTriggerImageTags1` - Any tags assigned to the image
  * `awsAmiTriggerImageIsPublic1` - Indicates whether the image has public launch permissions

Also, the values of the triggered filter are available as:

  * `awsAmiTriggerFilterArchitecture1`
  * `awsAmiTriggerFilterDescription1`
  * `awsAmiTriggerFilterName1`
  * `awsAmiTriggerFilterOwnerAlias1`
  * `awsAmiTriggerFilterOwnerId1`
  * `awsAmiTriggerFilterProductCode1`
  * `awsAmiTriggerFilterTags1`
  * `awsAmiTriggerFilterIsPublic1`

No variables are set for filters that did not match any new AMIs.
